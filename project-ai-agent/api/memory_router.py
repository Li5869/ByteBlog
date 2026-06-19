"""
记忆提取 API 路由
文件：project-ai-agent/api/memory_router.py

提供对话记忆批量提取接口，由 Java XXL-Job 定时调用。
具体业务逻辑封装在 services/business/memory_service.py 中。
"""

from fastapi import APIRouter
from loguru import logger

from models.schemas import ApiResponse, MemoryExtractRequest
from services.business.memory_service import get_memory_extract_service

router = APIRouter()


@router.post("/extract")
async def extract_memories(request: MemoryExtractRequest):
    """
    批量提取对话记忆（由 Java XXL-Job 调度调用）

    Args:
        request: 待提取的对话列表 [{conversation_id, user_id}, ...]

    Returns:
        每条对话的处理结果
    """
    service = get_memory_extract_service()

    # 记忆功能未启用时直接拒绝
    if not service.is_enabled:
        logger.warning("[记忆提取] 记忆功能未启用，拒绝请求")
        return ApiResponse(code=503, msg="记忆功能未启用")

    results = await service.extract_memories(request.conversations)
    return ApiResponse(data={"results": results})
