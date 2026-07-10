"""
对话业务服务
文件：project-ai-agent/services/business/chat_service.py

封装对话的业务逻辑，包括：
- 对话历史拼接
- 消息保存
- 任务注册/清理
- 中断保存处理
"""

import asyncio
import json
import re
from datetime import datetime
from typing import Dict, Optional, AsyncGenerator

from loguru import logger

from agents.smart_agent import get_smart_agent, StreamEvent
from common.constants import REDIS_KEY_CHAT_ACTIVE_PREFIX
from config.settings import get_settings
from services.core.memory_service import get_memory_service, ToolCall


class ChatService:
    """对话业务服务"""

    def __init__(self):
        self._chat_tasks: Dict[str, asyncio.Task] = {}
        self._memory_service = get_memory_service()
        self._agent = get_smart_agent()

    async def prepare_conversation(
        self, conversation_id: str, message: str
    ) -> tuple[str, str]:
        """
        准备对话：保存用户消息到 Redis（供 UI 展示和长期记忆提取使用）。

        注意：对话上下文不再手动拼接，由 LangGraph 持久化 checkpointer 
        (AsyncRedisSaver) 通过 thread_id 自动恢复完整消息历史。

        Args:
            conversation_id: 会话 ID
            message: 用户消息

        Returns:
            (message, conversation_id) — message 为原始用户消息，不再拼接历史
        """
        await self._memory_service.add_message(conversation_id, "user", message)
        return message, conversation_id

    def register_task(self, conversation_id: str, task: asyncio.Task):
        """注册活跃任务"""
        if task:
            self._chat_tasks[conversation_id] = task

    def unregister_task(self, conversation_id: str):
        """清理任务注册"""
        self._chat_tasks.pop(conversation_id, None)

    def cancel_task(self, conversation_id: str) -> bool:
        """取消活跃任务"""
        task = self._chat_tasks.get(conversation_id)
        if task and not task.done():
            task.cancel()
            logger.info(f"[ChatService] 已取消对话任务 (conversation: {conversation_id})")
            return True
        return False

    async def save_message(
        self,
        conversation_id: str,
        content: str,
        thinking: Optional[str] = None,
        tool_calls: Optional[list] = None,
    ):
        """保存助手消息"""
        tool_calls_models = None
        if tool_calls:
            tool_calls_models = [
                ToolCall(id=i, name=tc["name"], args=tc.get("args", {}), result=tc.get("result"))
                for i, tc in enumerate(tool_calls)
            ]

        await self._memory_service.add_message(
            conversation_id, "assistant",
            content=content,
            thinking=thinking or None,
            tool_calls=tool_calls_models,
        )

    async def stream_chat(
        self,
        conversation_id: str,
        message: str,
        user_id: Optional[str] = None,
    ) -> AsyncGenerator[str, None]:
        """
        流式对话：封装完整的流式对话逻辑

        Args:
            conversation_id: 会话 ID
            message: 用户消息
            user_id: 用户 ID

        Yields:
            SSE 格式的字符串
        """
        enhanced_message, conv_id = await self.prepare_conversation(conversation_id, message)
        saved = False

        async def _save_partial(event: StreamEvent):
            """中断时保存部分响应"""
            nonlocal saved
            if saved:
                return
            saved = True
            if event.accumulated_response or event.accumulated_thinking:
                await self.save_message(
                    conv_id,
                    content=event.accumulated_response,
                    thinking=event.accumulated_thinking,
                    tool_calls=event.tool_calls_summary,
                )
                logger.info(f"[ChatService] 中断保存: conversation={conv_id}")

        try:
            async for event in self._agent.astream_chat_with_result(
                enhanced_message, user_id=user_id, conversation_id=conv_id
            ):
                yield self._event_to_sse(event)

                if event.event_type == "done":
                    saved = True
                    await self.save_message(
                        conv_id,
                        content=event.accumulated_response or event.content,
                        thinking=event.accumulated_thinking,
                        tool_calls=event.tool_calls_summary,
                    )
                    # 写入 Redis 活跃标记，供 Java XXL-Job 扫描对话结束状态
                    await self._write_activity_marker(conv_id, user_id)

                elif event.event_type == "error":
                    await _save_partial(event)

        except asyncio.CancelledError:
            logger.warning(f"[ChatService] 对话被取消: conversation={conv_id}")
            raise

        except Exception as e:
            logger.error(f"[ChatService] 流式对话失败: {e}")
            yield f"data: {json.dumps({'type': 'error', 'content': str(e)}, ensure_ascii=False)}\n\n"

    def _event_to_sse(self, event: StreamEvent) -> str:
        """将事件转换为 SSE 格式"""
        data = event.to_sse_dict()

        # 处理 tool_call 中的 WRITING_TRIGGER
        if event.event_type == "tool_call" and event.extra:
            result = event.extra.get("result", "")
            trigger_match = re.search(r'<!-- WRITING_TRIGGER: \{.*} -->', result)
            if trigger_match:
                trigger_data = {
                    "type": "tool_call",
                    "content": trigger_match.group(0),
                    "tool_name": event.tool_name,
                }
                return f"data: {json.dumps(trigger_data, ensure_ascii=False)}\n\n"

        return f"data: {json.dumps(data, ensure_ascii=False)}\n\n"

    async def _write_activity_marker(self, conversation_id: str, user_id: Optional[str]):
        """
        写入对话活跃标记到 Redis

        Key:   chat:active:{conversation_id}
        Value: {"user_id": user_id, "last_active": 当前时间戳}
        TTL:   300s（大于 XXL-Job 扫描间隔 180s，防止误删）

        仅在 user_id 不为空时写入，未登录用户不触发记忆提取。
        """
        if not user_id:
            return

        try:
            settings = get_settings()
            key = f"{REDIS_KEY_CHAT_ACTIVE_PREFIX}{conversation_id}"
            value = json.dumps({
                "user_id": user_id,
                "last_active": datetime.now().isoformat(),
            })
            redis_client = await self._memory_service._get_client()
            await redis_client.set(key, value, ex=settings.memory_activity_marker_ttl)
            logger.debug(f"[活跃标记] 已写入: {key}")
        except Exception as e:
            # 写入标记失败不影响主流程
            logger.warning(f"[活跃标记] 写入失败: {e}")


_chat_service: Optional[ChatService] = None


def get_chat_service() -> ChatService:
    """获取对话服务单例"""
    global _chat_service
    if _chat_service is None:
        _chat_service = ChatService()
    return _chat_service