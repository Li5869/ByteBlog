"""
对话 API 路由（流式接口）
"""

import asyncio
import json
import uuid
from typing import List

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from loguru import logger

from agents.smart_agent import get_smart_agent
from models.schemas import ApiResponse, ChatRequest
from services.core.memory_service import get_memory_service, ToolCall
from tools import ALL_TOOLS

router = APIRouter()


# ==================== API 端点 ====================

@router.get("/history/{conversation_id}")
async def get_history(conversation_id: str, limit: int = 20):
    """
    获取对话历史（包含思考内容）

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
    流式对话接口

    返回格式（SSE）：
    - data: {"type": "reasoning", "content": "思考过程片段"}
    - data: {"type": "tool_call", "content": "调用工具名称", "tool_name": "xxx"}
    - data: {"type": "chunk", "content": "文本片段"}
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
        full_response = ""
        full_reasoning = ""
        tool_calls_list: List[ToolCall] = []
        saved = False

        async def _save_partial():
            """中断时保存部分响应"""
            nonlocal saved
            if saved:
                return
            saved = True
            if full_response:
                await memory_service.add_message(
                    conversation_id, "assistant",
                    content=full_response,
                    # 没开启深度思考时，不保存思考内容到 Redis，防止刷新后展示
                    thinking=full_reasoning if request.deep_thinking else None,
                    tool_calls=tool_calls_list or None,
                )
                logger.info(f"[中断保存] 已保存部分响应 (conversation: {conversation_id})")

        try:
            async for event in agent.astream_chat_with_result(
                enhanced_message, deep_thinking=request.deep_thinking, user_id=request.user_id
            ):
                if event.event_type == "reasoning":
                    if event.reasoning_content:
                        full_reasoning = event.reasoning_content
                    yield f"data: {json.dumps({'type': 'reasoning', 'content': event.content}, ensure_ascii=False)}\n\n"

                elif event.event_type == "token":
                    chunk = event.content
                    full_response += chunk
                    yield f"data: {json.dumps({'type': 'chunk', 'content': chunk}, ensure_ascii=False)}\n\n"

                elif event.event_type == "tool_call":
                    tool_calls_list.append(ToolCall(
                        id=len(tool_calls_list),
                        name=event.tool_name,
                        args=event.tool_args,
                    ))
                    yield f"data: {json.dumps({'type': 'tool_call', 'content': event.content.strip(), 'tool_name': event.tool_name}, ensure_ascii=False)}\n\n"

                elif event.event_type == "done":
                    final_content = event.content or full_response
                    final_reasoning = event.reasoning_content or full_reasoning
                    await memory_service.add_message(
                        conversation_id, "assistant",
                        content=final_content,
                        # 没开启深度思考时，不保存思考内容到 Redis，防止刷新后展示
                        thinking=final_reasoning if request.deep_thinking else None,
                        tool_calls=tool_calls_list or None,
                    )
                    saved = True
                    yield f"data: {json.dumps({'type': 'done', 'conversation_id': conversation_id}, ensure_ascii=False)}\n\n"

        except GeneratorExit:
            await _save_partial()
            raise

        except asyncio.CancelledError:
            await _save_partial()
            logger.warning(f"[中断] 客户端断开连接 (conversation: {conversation_id})")
            raise

        except Exception as e:
            logger.error(f"流式对话失败: {e}")
            await _save_partial()
            yield f"data: {json.dumps({'type': 'error', 'content': str(e)}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
