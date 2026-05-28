"""
对话 API 路由（流式接口）
"""

import asyncio
import uuid

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from loguru import logger

from models.schemas import ApiResponse, ChatRequest
from services.business.chat_service import get_chat_service
from services.core.memory_service import get_memory_service
from tools import ALL_TOOLS

router = APIRouter()


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

    SSE 事件类型：
    - thinking: 思考分析内容 → 前端追加到 thinking 面板
    - tool_call: 工具调用记录 → 前端追加到 thinking 面板
    - chunk: 最终回答文本片段 → 前端追加到回答区域
    - done: 对话完成信号
    """
    conversation_id = request.conversation_id or str(uuid.uuid4())
    chat_service = get_chat_service()

    async def generate():
        current_task = asyncio.current_task()
        if current_task:
            chat_service.register_task(conversation_id, current_task)

        try:
            async for sse_data in chat_service.stream_chat(
                conversation_id, request.message, user_id=request.user_id
            ):
                yield sse_data

        except asyncio.CancelledError:
            logger.warning(f"[中断] 对话被取消 (conversation: {conversation_id})")

        finally:
            chat_service.unregister_task(conversation_id)

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
    """停止正在进行的对话"""
    chat_service = get_chat_service()
    if chat_service.cancel_task(conversation_id):
        return ApiResponse(msg="对话已停止")
    return ApiResponse(code=404, msg="没有正在进行的对话")
