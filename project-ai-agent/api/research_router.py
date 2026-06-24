"""
深度研究 API 路由

1. 接收 HTTP 请求，调用 ResearchAgent
2. 通过 LangGraph StreamWriter 实现 SSE 事件推送
3. 任务生命周期管理（启动/恢复/停止）
"""

import json
from typing import Optional

from fastapi import APIRouter
from fastapi.responses import StreamingResponse, JSONResponse
from loguru import logger

from agents.research_agent import get_research_agent
from models.research_models import StartRequest, ResumeRequest, StopRequest
from models.schemas import ApiResponse

router = APIRouter(tags=["Research"])


@router.post("/start")
async def start_research(request: StartRequest):
    """
    启动深度研究任务

    SSE 事件流返回，前端通过 EventSource 接收事件。
    """
    try:
        research_agent = get_research_agent()
        task_id = request.task_id

        logger.info(f"[Research API] 启动研究: task_id={task_id}, 需求={request.message[:50]}...")

        async def event_generator():
            """SSE 事件生成器"""
            try:
                async for event in research_agent.start_research(
                    user_request=request.message,
                    thread_id=task_id,
                ):
                    event_data = json.dumps(event, ensure_ascii=False)
                    yield f"data: {event_data}\n\n"
            except Exception as e:
                logger.error(f"[Research API] SSE 事件生成异常: {e}")
                error_event = json.dumps(
                    {"type": "error", "data": {"message": str(e)}},
                    ensure_ascii=False,
                )
                yield f"data: {error_event}\n\n"

        return StreamingResponse(
            event_generator(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no",
            },
        )

    except Exception as e:
        logger.error(f"[Research API] 启动研究失败: {e}")
        return JSONResponse(
            status_code=500,
            content={"code": 500, "msg": str(e)},
        )


@router.post("/resume")
async def resume_research(request: ResumeRequest):
    """
    恢复被中断的研究任务

    用于回答澄清问题、确认计划或提供修改意见。
    SSE 事件流返回。
    """
    try:
        research_agent = get_research_agent()
        task_id = request.task_id

        logger.info(f"[Research API] 恢复研究: task_id={task_id}")

        # 解析用户响应
        response = request.response
        if response == "approve":
            response = True
        elif response.startswith("{") or response.startswith("["):
            try:
                response = json.loads(response)
            except json.JSONDecodeError:
                pass

        async def event_generator():
            """SSE 事件生成器"""
            try:
                async for event in research_agent.resume_research(
                    thread_id=task_id,
                    response=response,
                ):
                    event_data = json.dumps(event, ensure_ascii=False)
                    yield f"data: {event_data}\n\n"
            except Exception as e:
                logger.error(f"[Research API] SSE 事件生成异常: {e}")
                error_event = json.dumps(
                    {"type": "error", "data": {"message": str(e)}},
                    ensure_ascii=False,
                )
                yield f"data: {error_event}\n\n"

        return StreamingResponse(
            event_generator(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no",
            },
        )

    except Exception as e:
        logger.error(f"[Research API] 恢复研究失败: {e}")
        return JSONResponse(
            status_code=500,
            content={"code": 500, "msg": str(e)},
        )


@router.post("/stop")
async def stop_research(request: StopRequest):
    """
    停止研究任务

    注意：LangGraph 的 interrupt/resume 机制不支持强制中止正在执行的节点。
    此接口主要用于标记任务为停止状态，下次 Replanner 检查时会停止。
    """
    try:
        task_id = request.task_id
        logger.info(f"[Research API] 停止研究: task_id={task_id}")

        # TODO: 实际的停止逻辑需要通过 Redis 或数据库标记任务状态
        # 当前先返回成功，后续实现持久化时补充

        return ApiResponse(data={"task_id": task_id, "status": "stopped"})

    except Exception as e:
        logger.error(f"[Research API] 停止研究失败: {e}")
        return ApiResponse(code=500, msg=str(e))
