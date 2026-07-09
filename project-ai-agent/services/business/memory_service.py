"""
记忆提取业务服务
文件：project-ai-agent/services/business/memory_service.py

封装记忆提取的业务逻辑，供 Memory API 路由调用。
包括：读取对话历史、格式转换、调用 Mem0 提取记忆。
"""

from typing import Optional, List, Dict, Any

from loguru import logger

from config.settings import get_settings
from models.schemas import MemoryExtractItem
from services.core.memory_service import get_memory_service
from services.core.long_term_memory_service import get_long_term_memory_service
def _sanitize_memory_content(content: str) -> str:
    """
    清洗记忆内容，移除会导致 Mem0 内部 LLM extraction JSON 解析失败的字符。

    Mem0 的 extraction 流程会让 LLM 生成 JSON，中文双引号等特殊字符
    容易导致 LLM 输出畸形 JSON（如未转义的 ASCII 引号），从而使解析失败。
    将中文双引号替换为更安全的「」括号，语义不变但不会破坏 JSON 结构。

    注意：此函数仅用于 extraction 场景（infer=True）。对于已经是提炼好事实的内容，
    应使用 infer=False 跳过 extraction，完全避免此问题。
    """
    content = content.replace("\u201c", "\u300c").replace("\u201d", "\u300d")
    content = content.replace("\u2018", "\u300e").replace("\u2019", "\u300f")
    return content


class MemoryExtractService:
    """
    记忆提取业务服务

    职责：从 Redis 读取对话历史 → 转换消息格式 → 调用 Mem0 提取并存储记忆
    """

    def __init__(self):
        self._memory_service = get_memory_service()
        self._ltm_service = get_long_term_memory_service()
        self._settings = get_settings()

    @property
    def is_enabled(self) -> bool:
        """记忆功能是否启用"""
        return self._settings.memory_enabled

    async def extract_memories(
        self, conversations: List[MemoryExtractItem]
    ) -> List[Dict[str, Any]]:
        """
        批量提取对话记忆

        遍历每个对话，从 Redis 读取消息历史，调用 Mem0 提取关键记忆。

        Args:
            conversations: 待提取的对话列表

        Returns:
            每条对话的处理结果列表
        """
        results = []

        for item in conversations:
            result = await self._extract_single(item.conversation_id, item.user_id)
            results.append(result)

        self._log_summary(results)
        return results

    async def _extract_single(
        self, conversation_id: str, user_id: str
    ) -> Dict[str, Any]:
        """
        提取单个对话的记忆

        Args:
            conversation_id: 会话 ID
            user_id: 用户 ID

        Returns:
            处理结果 {"conversation_id", "status", "error"}
        """
        result = {
            "conversation_id": conversation_id,
            "status": "success",
            "error": None,
        }

        try:
            # 读取 Redis 中的对话历史消息（异步调用）
            messages = await self._memory_service.get_messages(conversation_id)
            if not messages:
                logger.info(f"[记忆提取] 对话 {conversation_id} 无历史消息，跳过")
                result["status"] = "skipped"
                return result

            # 转换为 Mem0 需要的消息格式
            mem0_messages = self._convert_messages(messages)

            # 调用 Mem0 提取并存储记忆
            await self._ltm_service.add_memory(
                messages=mem0_messages,
                user_id=user_id,
                metadata={"conversation_id": conversation_id},
            )

            logger.info(
                f"[记忆提取] 对话 {conversation_id} 提取成功 "
                f"(user_id={user_id}, {len(mem0_messages)} 条消息)"
            )

        except Exception as e:
            logger.error(f"[记忆提取] 对话 {conversation_id} 提取失败: {e}")
            result["status"] = "error"
            result["error"] = str(e)

        return result

    @staticmethod
    def _convert_messages(messages) -> List[Dict[str, str]]:
        """
        将 Redis 消息转换为 Mem0 需要的格式

        如果消息包含思考过程（thinking），将其拼接到 content 中，
        让 Mem0 能提取 Agent 的推理过程中的关键信息。

        Args:
            messages: Redis 中存储的消息列表

        Returns:
            [{"role": "user", "content": "..."}, ...]
        """
        mem0_messages = []
        for msg in messages:
            role = msg.role
            content = msg.content or ""
            # 如果有思考过程，拼接到 content 中供 Mem0 提取
            if hasattr(msg, "thinking") and msg.thinking:
                content = f"[思考]\n{msg.thinking}\n\n[回答]\n{content}"
            mem0_messages.append({"role": role, "content": _sanitize_memory_content(content)})
        return mem0_messages

    @staticmethod
    def _log_summary(results: List[Dict[str, Any]]):
        """记录处理汇总日志"""
        success_count = sum(1 for r in results if r["status"] == "success")
        skipped_count = sum(1 for r in results if r["status"] == "skipped")
        error_count = sum(1 for r in results if r["status"] == "error")
        logger.info(
            f"[记忆提取] 处理完成: 成功={success_count}, "
            f"跳过={skipped_count}, 失败={error_count}"
        )


_memory_extract_service: Optional[MemoryExtractService] = None


def get_memory_extract_service() -> MemoryExtractService:
    """获取记忆提取服务单例"""
    global _memory_extract_service
    if _memory_extract_service is None:
        _memory_extract_service = MemoryExtractService()
    return _memory_extract_service
