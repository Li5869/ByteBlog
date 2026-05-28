"""
智能写作 API 路由

1. 接收 HTTP 请求，调用写作执行服务
2. 通过 Redis Stream 实现 SSE 事件推送
3. 任务生命周期管理（创建/恢复/取消/停止/完成）
"""

import asyncio
import json
from datetime import datetime

from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse, JSONResponse
from loguru import logger

from services.business.writing.writing_execution_service import (
    _get_redis, _task_key, _events_key, _plan_key,
    start_plan_phase, cancel_task, stop_task, finalize_task,
    handle_approve, handle_revise,
)
from models.writing_models import (
    StartRequest, ResumeRequest,
    StopRequest, FinalizeRequest, CancelRequest,
)
from models.schemas import ApiResponse
from common.constants import WRITING_TASK_TTL_SECONDS

router = APIRouter(tags=["Writing"])

@router.post("/start")
async def start_writing(request: StartRequest):
    """启动写作任务 - 阶段1：仅生成写作计划"""
    try:
        task_id = request.task_id
        redis = await _get_redis()

        await redis.hset(_task_key(task_id), mapping={
            "status": "planning",
            "step": "generating_plan",
            "user_request": request.message,
            "created_at": datetime.now().isoformat(),
            "error": "",
        })
        await redis.expire(_task_key(task_id), WRITING_TASK_TTL_SECONDS)

        success = await start_plan_phase(task_id, request.message)
        if not success:
            return ApiResponse(code=500, msg="启动计划生成失败")

        logger.info(f"[Writing] 启动写作任务: {task_id}")
        return ApiResponse(data={"task_id": task_id, "status": "planning"})

    except Exception as e:
        logger.error(f"[Writing] 启动任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))


@router.post("/{task_id}/resume")
async def resume_writing(task_id: str, request: ResumeRequest):
    """恢复写作任务（用户确认计划或要求修改）"""
    try:
        redis = await _get_redis()
        task_key = _task_key(task_id)
        plan_key = _plan_key(task_id)
        
        logger.info(f"[Writing] 恢复任务检查, taskId={task_id}, taskKey={task_key}")
        
        # 检查 task 是否存在
        if not await redis.exists(task_key):
            # 如果 task 不存在但 plan 存在，尝试恢复 task 数据
            plan_json = await redis.get(plan_key)
            if plan_json:
                logger.info(f"[Writing] task不存在但plan存在，尝试恢复, taskId={task_id}")
                # 恢复 task 数据
                await redis.hset(task_key, mapping={
                    "status": "plan_ready",
                    "step": "awaiting_approval",
                    "created_at": datetime.now().isoformat(),
                    "error": "",
                })
                await redis.expire(task_key, WRITING_TASK_TTL_SECONDS)
            else:
                logger.warning(f"[Writing] 任务不存在, taskId={task_id}, taskKey={task_key}")
                return ApiResponse(code=404, msg="任务不存在，请重新开始写作")

        if request.action == "approve":
            result = await handle_approve(task_id)
        elif request.action == "revise":
            result = await handle_revise(task_id, request.feedback or "")
        else:
            return ApiResponse(code=400, msg="无效的action参数，请使用 approve 或 revise")

        if not result["success"]:
            return ApiResponse(code=500, msg=result["error"])
        
        return ApiResponse(data={"task_id": task_id, "status": result["status"]})

    except Exception as e:
        logger.error(f"[Writing] 恢复任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))


@router.get("/{task_id}/stream")
async def stream_writing(task_id: str, request: Request):
    """SSE 流式获取写作进度"""
    try:
        redis = await _get_redis()

        if not await redis.exists(_task_key(task_id)):
            return JSONResponse(status_code=404, content={"code": 404, "msg": "任务不存在"})

        async def event_generator():
            last_id = "0"
            terminal_status = None
            # 心跳计数器，block=1000ms 时，30 次约 30 秒发送一次心跳
            heartbeat_counter = 0

            try:
                while True:
                    if await request.is_disconnected():
                        logger.info(f"[Writing] 客户端断开连接: {task_id}")
                        break

                    events = await redis.xread(
                        {_events_key(task_id): last_id}, count=10, block=1000
                    )

                    if events:
                        # 有事件时重置心跳计数器
                        heartbeat_counter = 0
                        for stream, messages in events:
                            for msg_id, msg in messages:
                                last_id = msg_id
                                event_data = msg["event"]
                                yield f"data: {event_data}\n\n"

                                try:
                                    event_obj = json.loads(event_data)
                                    if event_obj.get("type") in ("plan_ready", "done"):
                                        terminal_status = event_obj["type"]
                                except Exception:
                                    pass
                    else:
                        # 无事件时累加心跳计数器
                        heartbeat_counter += 1
                        if heartbeat_counter >= 30:
                            yield ': heartbeat\n\n'
                            heartbeat_counter = 0

                    if terminal_status:
                        break

                    task_status = await redis.hget(_task_key(task_id), "status")
                    if task_status in ("cancelled", "stopped", "error"):
                        await asyncio.sleep(0.3)
                        break

            except Exception as e:
                logger.error(f"[Writing] SSE 事件生成异常: {e}")
                yield f'data: {{"type":"error","message":"服务内部错误"}}\n\n'

            finally:
                # 清理已完成任务资源
                # 只有在任务失败、取消或停止时才删除 task 数据
                # plan_ready 状态需要保留，以便用户可以回来确认计划
                try:
                    task_status = await redis.hget(_task_key(task_id), "status")
                    if task_status in ("cancelled", "stopped", "error"):
                        await redis.delete(_events_key(task_id))
                        await redis.delete(_task_key(task_id))
                    elif task_status == "finalized":
                        # 任务完成，清理 events 但保留 task 记录
                        await redis.delete(_events_key(task_id))
                except Exception as e:
                    logger.error(f"[Writing] 清理资源失败: {e}")

        return StreamingResponse(
            event_generator(),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no"},
        )

    except Exception as e:
        logger.error(f"[Writing] SSE 流异常: {e}")
        return JSONResponse(status_code=500, content={"code": 500, "msg": str(e)})


@router.get("/{task_id}/status")
async def get_status(task_id: str):
    """获取任务状态"""
    try:
        redis = await _get_redis()

        if not await redis.exists(_task_key(task_id)):
            return ApiResponse(code=404, msg="任务不存在")

        task_state = await redis.hgetall(_task_key(task_id))
        return ApiResponse(data={
            "task_id": task_id,
            "status": task_state.get("status", "unknown"),
            "step": task_state.get("step", ""),
            "error": task_state.get("error", ""),
        })

    except Exception as e:
        logger.error(f"[Writing] 获取状态失败: {e}")
        return ApiResponse(code=500, msg=str(e))


@router.post("/{task_id}/cancel")
async def cancel_writing(task_id: str, request: CancelRequest):
    """取消写作任务"""
    try:
        success = await cancel_task(task_id, request.reason or "用户取消")
        if not success:
            return ApiResponse(code=404, msg="任务不存在")

        logger.info(f"[Writing] 取消任务: {task_id}")
        return ApiResponse(data={"task_id": task_id, "status": "cancelled"})

    except Exception as e:
        logger.error(f"[Writing] 取消任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))


@router.post("/{task_id}/stop")
async def stop_writing(task_id: str, request: StopRequest):
    """停止写作任务"""
    try:
        success = await stop_task(task_id, request.save_partial)
        if not success:
            return ApiResponse(code=404, msg="任务不存在")

        logger.info(f"[Writing] 停止任务: {task_id}")
        return ApiResponse(data={"task_id": task_id, "status": "stopped"})

    except Exception as e:
        logger.error(f"[Writing] 停止任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))


@router.post("/{task_id}/finalize")
async def finalize_writing(task_id: str):
    """完成写作任务"""
    try:
        success = await finalize_task(task_id)
        if not success:
            return ApiResponse(code=404, msg="任务不存在")

        logger.info(f"[Writing] 完成任务: {task_id}")
        return ApiResponse(data={"task_id": task_id, "status": "finalized"})

    except Exception as e:
        logger.error(f"[Writing] 完成任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))
