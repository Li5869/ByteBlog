"""
对话 API 路由（流式接口）
"""

import asyncio
import json
import re
import uuid
from typing import Dict, List

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from loguru import logger

from agents.smart_agent import get_smart_agent
from models.schemas import ApiResponse, ChatRequest
from services.core.memory_service import get_memory_service, ToolCall
from tools import ALL_TOOLS

router = APIRouter()

# 活跃对话任务注册表，用于外部取消（参考 writing_router 的 _writing_tasks）
_chat_tasks: Dict[str, asyncio.Task] = {}


# ==================== API 端点 ====================

@router.get("/history/{conversation_id}")
async def get_history(conversation_id: str, limit: int = 20):
    """
    获取对话历史（包含思考过程和工具调用记录）

    Args:
        conversation_id: 会话 ID
        limit: 最大消息数量，默认 20
    """
    try:
        memory_service = get_memory_service()
        messages = memory_service.get_messages(conversation_id, limit)

        return ApiResponse(data={
            "conversation_id": conversation_id,
            "messages": [msg.model_dump() for msg in messages]
        })

    except Exception as e:
        logger.error(f"获取历史失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/history/{conversation_id}")
async def clear_history(conversation_id: str):
    """清空对话历史"""
    try:
        memory_service = get_memory_service()
        await memory_service.clear_memory(conversation_id)
        return ApiResponse(msg="对话历史已清空")

    except Exception as e:
        logger.error(f"清空历史失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/tools")
async def list_tools():
    """获取可用工具列表"""
    tools_info = [
        {"name": tool.name, "description": tool.description}
        for tool in ALL_TOOLS
    ]
    return ApiResponse(data={"tools": tools_info})


@router.post("/stream")
async def stream_message(request: ChatRequest):
    """
    流式对话接口（纯 ReAct 范式，思考/回答分阶段）

    ReAct 循环的思考/回答分界：
      - 模型分析规划阶段 → thinking 事件（前端展示在"思考过程"面板）
      - 模型拿到工具结果后 → chunk 事件（前端展示在回答区域，打字机效果）

    SSE 事件类型：
    - thinking: 思考分析内容 → 前端追加到 thinking 面板
    - tool_call: 工具调用记录 → 前端追加到 thinking 面板
    - chunk: 最终回答文本片段 → 前端追加到回答区域
    - done: 对话完成信号

    返回格式（SSE）：
    - data: {"type": "thinking", "content": "思考文本片段"}
    - data: {"type": "tool_call", "content": "调用工具名称", "tool_name": "xxx"}
    - data: {"type": "chunk", "content": "回答文本片段"}
    - data: {"type": "done", "conversation_id": "xxx"}
    """
    conversation_id = request.conversation_id or str(uuid.uuid4())
    memory_service = get_memory_service()
    agent = get_smart_agent()

    # 拼接对话历史到当前消息（类似 Java 的 ContextBuilder）
    history = await memory_service.get_context_string(conversation_id, limit=10)
    enhanced_message = (
        f"[对话历史]\n{history}\n\n[当前问题]\n{request.message}"
        if history else request.message
    )

    # 保存用户消息
    await memory_service.add_message(conversation_id, "user", request.message)

    async def generate():
        full_response = ""    # 最终回答
        full_thinking = ""    # 思考过程
        tool_calls_list: List[ToolCall] = []
        saved = False

        # 注册当前任务，供外部 stop 端点取消
        current_task = asyncio.current_task()
        if current_task:
            _chat_tasks[conversation_id] = current_task

        async def _save_partial():
            """中断时保存部分响应"""
            nonlocal saved
            if saved:
                return
            saved = True
            if full_response or full_thinking:
                await memory_service.add_message(
                    conversation_id, "assistant",
                    content=full_response,
                    thinking=full_thinking or None,
                    tool_calls=tool_calls_list or None,
                )
                logger.info(f"[中断保存] 已保存部分响应 (conversation: {conversation_id})")

        try:
            async for event in agent.astream_chat_with_result(
                enhanced_message, user_id=request.user_id
            ):
                if event.event_type == "thinking":
                    # 思考分析内容 → 前端展示在思考过程面板
                    thinking_chunk = event.content
                    full_thinking += thinking_chunk
                    yield f"data: {json.dumps({'type': 'thinking', 'content': thinking_chunk}, ensure_ascii=False)}\n\n"

                elif event.event_type == "token":
                    # 最终回答文本片段 → 前端打字机效果展示
                    chunk = event.content
                    full_response += chunk
                    yield f"data: {json.dumps({'type': 'chunk', 'content': chunk}, ensure_ascii=False)}\n\n"

                elif event.event_type == "tool_call":
                    # 工具调用记录 → 前端追加到思考面板
                    tool_calls_list.append(ToolCall(
                        id=len(tool_calls_list),
                        name=event.tool_name,
                        args=event.tool_args,
                    ))
                    full_thinking += event.content
                    yield f"data: {json.dumps({'type': 'tool_call', 'content': event.content.strip(), 'tool_name': event.tool_name}, ensure_ascii=False)}\n\n"

                elif event.event_type == "tool_result":
                    # 工具执行结果 → 更新到 tool_calls_list（后续持久化到 Redis）
                    tool_name = event.tool_name
                    result = event.extra.get("result", "") if event.extra else ""
                    for tc in reversed(tool_calls_list):
                        if tc.name == tool_name and tc.result is None:
                            tc.result = result
                            break
                    # 提取 WRITING_TRIGGER 标记，以 tool_call 类型发送到前端
                    trigger_match = re.search(r'<!-- WRITING_TRIGGER: \{.*\} -->', result)
                    if trigger_match:
                        yield f"data: {json.dumps({'type': 'tool_call', 'content': trigger_match.group(0), 'tool_name': tool_name}, ensure_ascii=False)}\n\n"

                elif event.event_type == "done":
                    final_content = event.content or full_response
                    await memory_service.add_message(
                        conversation_id, "assistant",
                        content=final_content,
                        thinking=full_thinking or None,
                        tool_calls=tool_calls_list or None,
                    )
                    saved = True
                    yield f"data: {json.dumps({'type': 'done', 'conversation_id': conversation_id, 'tool_calls': [tc.model_dump() for tc in tool_calls_list]}, ensure_ascii=False)}\n\n"

        except GeneratorExit:
            await _save_partial()
            raise

        except asyncio.CancelledError:
            await _save_partial()
            logger.warning(f"[中断] 对话被取消 (conversation: {conversation_id})")
            raise

        except Exception as e:
            logger.error(f"流式对话失败: {e}")
            await _save_partial()
            yield f"data: {json.dumps({'type': 'error', 'content': str(e)}, ensure_ascii=False)}\n\n"

        finally:
            # 清理任务注册表
            _chat_tasks.pop(conversation_id, None)

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@router.post("/stop/{conversation_id}")
async def stop_chat(conversation_id: str):
    """停止正在进行的对话（取消 asyncio Task）"""
    task = _chat_tasks.get(conversation_id)
    if task and not task.done():
        task.cancel()
        logger.info(f"[停止] 已取消对话任务 (conversation: {conversation_id})")
        return ApiResponse(msg="对话已停止")
    return ApiResponse(code=404, msg="没有正在进行的对话")
