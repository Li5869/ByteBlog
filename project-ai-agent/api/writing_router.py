"""
智能写作 API 路由

1. 接收 HTTP 请求，调用 Agent 服务
2. 通过 Redis Stream 实现 SSE 事件推送
3. 任务生命周期管理（创建/恢复/取消/停止/完成）
"""

import asyncio
import json
from datetime import datetime
from typing import Dict

from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse, JSONResponse
from loguru import logger

from services.core.memory_service import get_memory_service
from services.business.writing.writing_task_service import get_writing_task_service
from agents.writing_agent import get_writing_agent
from models.writing_models import (
    WritingPlan, StartRequest, ResumeRequest,
    StopRequest, FinalizeRequest, CancelRequest,
)
from models.schemas import ApiResponse
from common.constants import (
    REDIS_KEY_WRITING_TASK_PREFIX,
    REDIS_KEY_WRITING_EVENTS_PREFIX,
    REDIS_KEY_WRITING_PLAN_PREFIX,
    WRITING_TASK_TTL_SECONDS,
)

router = APIRouter(tags=["Writing"])

# 全局任务注册表（类似 Java 的 ConcurrentHashMap<taskId, Future>）
_writing_tasks: Dict[str, asyncio.Task] = {}


# ==================== Redis 辅助方法 ====================

async def _get_redis():
    """获取 Redis 客户端（异步）"""
    return await get_memory_service()._get_client()


def _task_key(task_id: str) -> str:
    return f"{REDIS_KEY_WRITING_TASK_PREFIX}{task_id}"


def _events_key(task_id: str) -> str:
    return f"{REDIS_KEY_WRITING_EVENTS_PREFIX}{task_id}"


def _plan_key(task_id: str) -> str:
    return f"{REDIS_KEY_WRITING_PLAN_PREFIX}{task_id}"


async def _push_event(task_id: str, event: dict):
    """推送事件到 Redis Stream"""
    redis = await _get_redis()
    await redis.xadd(_events_key(task_id), {"event": json.dumps(event, ensure_ascii=False)})
    await redis.expire(_events_key(task_id), WRITING_TASK_TTL_SECONDS)


async def _set_task_status(task_id: str, **fields):
    """更新任务状态"""
    redis = await _get_redis()
    await redis.hset(_task_key(task_id), mapping=fields)


def _cleanup_task(task_id: str):
    """从全局注册表移除任务"""
    if task_id in _writing_tasks:
        del _writing_tasks[task_id]


# ==================== API 端点 ====================

@router.post("/start")
async def start_writing(request: StartRequest):
    """启动写作任务 - 阶段1：仅生成写作计划"""
    try:
        task_service = get_writing_task_service()
        
        task_id = request.task_id
        db_task_id = int(task_id)
        
        await task_service.update_status(db_task_id, "planning", "generating_plan")
        
        redis = await _get_redis()

        await redis.hset(_task_key(task_id), mapping={
            "status": "planning",
            "step": "generating_plan",
            "user_request": request.message,
            "created_at": datetime.now().isoformat(),
            "error": "",
        })
        await redis.expire(_task_key(task_id), WRITING_TASK_TTL_SECONDS)

        initial_state = {
            "user_request": request.message,
            "plan": None, "plan_approved": False, "plan_feedback": None,
            "current_step": None, "writing_result": None, "references": None,
            "reflection": None, "revision_count": 0, "article_id": None,
            "action": None, "cover": None, "error": None,
        }

        task = asyncio.create_task(_run_plan_phase(task_id, initial_state))
        _writing_tasks[task_id] = task

        logger.info(f"[Writing] 启动写作任务: {task_id}, dbTaskId: {db_task_id}")

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
            return await _handle_approve(task_id, redis)
        elif request.action == "revise":
            return await _handle_revise(task_id, request, redis)
        else:
            return ApiResponse(code=400, msg="无效的action参数，请使用 approve 或 revise")

    except Exception as e:
        logger.error(f"[Writing] 恢复任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))


async def _handle_approve(task_id: str, redis):
    """处理用户批准计划"""
    plan_json = await redis.get(_plan_key(task_id))
    if not plan_json:
        return ApiResponse(code=400, msg="计划不存在，请重新开始")

    await redis.delete(_events_key(task_id))
    await _set_task_status(task_id, status="executing", step="title")

    task_service = get_writing_task_service()
    db_task_id = int(task_id)
    latest_plan = await task_service.get_latest_plan(db_task_id)
    if latest_plan and latest_plan.get("id"):
        await task_service.update_plan_approval_status(latest_plan["id"], "approved")

    task = asyncio.create_task(_run_execute_phase(task_id))
    _writing_tasks[task_id] = task

    logger.info(f"[Writing] 用户批准计划，开始执行: {task_id}")
    return ApiResponse(data={"task_id": task_id, "status": "executing"})


async def _handle_revise(task_id: str, request: ResumeRequest, redis):
    """处理用户修改计划"""
    if not request.feedback:
        return ApiResponse(code=400, msg="修改意见不能为空")

    await redis.delete(_events_key(task_id))
    await _set_task_status(task_id, status="planning", step="regenerating_plan")

    task_service = get_writing_task_service()
    db_task_id = int(task_id)
    latest_plan = await task_service.get_latest_plan(db_task_id)
    if latest_plan and latest_plan.get("id"):
        await task_service.update_plan_approval_status(latest_plan["id"], "rejected")

    user_request = await redis.hget(_task_key(task_id), "user_request")

    task = asyncio.create_task(
        _run_revise_plan_phase(task_id, user_request, request.feedback)
    )
    _writing_tasks[task_id] = task

    logger.info(f"[Writing] 用户要求修改计划: {task_id}")
    return ApiResponse(data={"task_id": task_id, "status": "planning"})


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

            while True:
                if await request.is_disconnected():
                    logger.info(f"[Writing] 客户端断开连接: {task_id}")
                    break

                events = await redis.xread(
                    {_events_key(task_id): last_id}, count=10, block=2000
                )

                if events:
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

                if terminal_status:
                    break

                task_status = await redis.hget(_task_key(task_id), "status")
                if task_status in ("cancelled", "stopped", "error"):
                    await asyncio.sleep(0.3)
                    break

            # 清理已完成任务资源
            # 只有在任务失败、取消或停止时才删除 task 数据
            # plan_ready 状态需要保留，以便用户可以回来确认计划
            task_status = await redis.hget(_task_key(task_id), "status")
            if task_status in ("cancelled", "stopped", "error"):
                await redis.delete(_events_key(task_id))
                await redis.delete(_task_key(task_id))
            elif task_status == "finalized":
                # 任务完成，清理 events 但保留 task 记录
                await redis.delete(_events_key(task_id))

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
        redis = await _get_redis()

        if not await redis.exists(_task_key(task_id)):
            return ApiResponse(code=404, msg="任务不存在")

        await _set_task_status(task_id, status="cancelled", error=request.reason or "用户取消")
        await redis.delete(_events_key(task_id))

        if task_id in _writing_tasks:
            _writing_tasks[task_id].cancel()
            _cleanup_task(task_id)

        logger.info(f"[Writing] 取消任务: {task_id}")
        return ApiResponse(data={"task_id": task_id, "status": "cancelled"})

    except Exception as e:
        logger.error(f"[Writing] 取消任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))


@router.post("/{task_id}/stop")
async def stop_writing(task_id: str, request: StopRequest):
    """停止写作任务"""
    try:
        redis = await _get_redis()

        if not await redis.exists(_task_key(task_id)):
            return ApiResponse(code=404, msg="任务不存在")

        await _set_task_status(task_id, status="stopped")

        if task_id in _writing_tasks:
            _writing_tasks[task_id].cancel()
            _cleanup_task(task_id)

        if not request.save_partial:
            await redis.delete(_events_key(task_id))

        logger.info(f"[Writing] 停止任务: {task_id}")
        return ApiResponse(data={"task_id": task_id, "status": "stopped"})

    except Exception as e:
        logger.error(f"[Writing] 停止任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))


@router.post("/{task_id}/finalize")
async def finalize_writing(task_id: str):
    """完成写作任务"""
    try:
        redis = await _get_redis()

        if not await redis.exists(_task_key(task_id)):
            return ApiResponse(code=404, msg="任务不存在")

        await _set_task_status(task_id, status="finalized")
        await redis.delete(_events_key(task_id))
        await redis.delete(_task_key(task_id))

        logger.info(f"[Writing] 完成任务: {task_id}")
        return ApiResponse(data={"task_id": task_id, "status": "finalized"})

    except Exception as e:
        logger.error(f"[Writing] 完成任务失败: {e}")
        return ApiResponse(code=500, msg=str(e))


# ==================== 后台任务 ====================

async def _run_plan_phase(task_id: str, initial_state: dict):
    """阶段1：生成写作计划，完成后等待用户确认"""
    try:
        agent = get_writing_agent()
        task_service = get_writing_task_service()
        redis = await _get_redis()

        db_task_id = int(task_id)
        await task_service.update_status(db_task_id, "planning", "generating_plan")

        async for event in agent.generate_plan(initial_state, thread_id=task_id):
            # 先更新状态，再推送事件，确保 SSE 流读取到正确的状态
            if event.get("type") == "plan_ready":
                await _set_task_status(task_id, status="plan_ready", step="awaiting_approval")
                await redis.set(
                    _plan_key(task_id),
                    json.dumps(event.get("data"), ensure_ascii=False),
                    ex=WRITING_TASK_TTL_SECONDS,
                )
                
                await task_service.save_plan(
                    db_task_id, 
                    event.get("data"), 
                    version=1
                )
                await task_service.update_status(db_task_id, "plan_ready", "awaiting_approval")
            
            # 推送事件（在状态更新之后）
            await _push_event(task_id, event)
            
            if event.get("type") == "plan_ready":
                break

            elif event.get("type") == "error":
                await _set_task_status(task_id, status="error", error=event.get("data", ""))
                await task_service.update_status(db_task_id, "error", None)
                break

    except asyncio.CancelledError:
        await _set_task_status(task_id, status="cancelled")
        await task_service.update_status(db_task_id, "cancelled", None)
    except Exception as e:
        logger.error(f"[Writing] 计划生成异常: {task_id}, {e}")
        await _set_task_status(task_id, status="error", error=str(e))
        await task_service.update_status(db_task_id, "error", None)
    finally:
        _cleanup_task(task_id)


async def _run_execute_phase(task_id: str):
    """阶段2：执行写作流程（标题→摘要→内容→标签→反思→完成）"""
    try:
        agent = get_writing_agent()
        task_service = get_writing_task_service()

        db_task_id = int(task_id)
        await task_service.update_status(db_task_id, "executing", "title")

        # 设置 progress_callback，在 _execute_node 子步骤执行时实时推送事件到 Redis
        async def progress_handler(event):
            await _push_event(task_id, event)
            if event.get("type") == "phase":
                phase_data = event.get("data", {})
                step = phase_data.get("step")
                if step:
                    await task_service.update_status(db_task_id, "executing", step)

        agent.progress_callback = progress_handler

        async for event in agent.execute_stream(thread_id=task_id):
            # 先更新状态，再推送事件
            if event.get("type") == "reflection_result":
                await task_service.update_status(db_task_id, "reflecting", "reflecting")

            elif event.get("type") == "finalize_ready":
                writing_result = event.get("data")
                await task_service.update_status(db_task_id, "finalized", "completed")
                
                task_info = await task_service.get_task(db_task_id)
                user_id = task_info.get("userId") if task_info else None
                
                draft_data = {
                    "title": writing_result.get("title"),
                    "summary": writing_result.get("summary"),
                    "content": writing_result.get("content"),
                    "cover": writing_result.get("cover"),
                    "category_id": writing_result.get("categoryId"),
                    "category_name": writing_result.get("categoryName"),
                    "tag_ids": writing_result.get("tagIds"),
                    "tag_names": writing_result.get("tagNames"),
                }
                await task_service.save_draft(db_task_id, user_id, draft_data)

            elif event.get("type") == "done":
                # 任务完成，等待用户确认发布或存草稿
                # finalAction 在用户操作时由 Java 后端设置
                await _set_task_status(task_id, status="finalized")

            elif event.get("type") == "error":
                await _set_task_status(task_id, status="error", error=event.get("data", ""))
                await task_service.update_status(db_task_id, "error", None)
            
            # 推送事件（在状态更新之后）
            await _push_event(task_id, event)
            
            if event.get("type") in ("done", "error"):
                break

    except asyncio.CancelledError:
        await _set_task_status(task_id, status="stopped")
        await task_service.update_status(db_task_id, "stopped", None)
    except Exception as e:
        logger.error(f"[Writing] 执行异常: {task_id}, {e}")
        await _set_task_status(task_id, status="error", error=str(e))
        await task_service.update_status(db_task_id, "error", None)
    finally:
        agent.progress_callback = None
        _cleanup_task(task_id)


async def _run_revise_plan_phase(task_id: str, user_request: str, plan_feedback: str):
    """根据用户反馈重新生成写作计划"""
    try:
        agent = get_writing_agent()
        task_service = get_writing_task_service()
        redis = await _get_redis()

        db_task_id = int(task_id)
        await task_service.update_status(db_task_id, "planning", "regenerating_plan")

        old_plan_json = await redis.get(_plan_key(task_id))
        old_plan = json.loads(old_plan_json) if old_plan_json else None
        old_plan_obj = WritingPlan(**old_plan) if old_plan else None

        latest_plan = await task_service.get_latest_plan(db_task_id)
        new_version = (latest_plan.get("version", 0) + 1) if latest_plan else 2

        initial_state = {
            "user_request": user_request,
            "plan": old_plan_obj, "plan_approved": False, "plan_feedback": plan_feedback,
            "current_step": None, "writing_result": None, "references": None,
            "reflection": None, "revision_count": 0, "article_id": None,
            "action": None, "cover": None, "error": None,
        }

        async for event in agent.generate_plan(initial_state, thread_id=task_id):
            # 先更新状态，再推送事件
            if event.get("type") == "plan_ready":
                await _set_task_status(task_id, status="plan_ready", step="awaiting_approval")
                await redis.set(
                    _plan_key(task_id),
                    json.dumps(event.get("data"), ensure_ascii=False),
                    ex=WRITING_TASK_TTL_SECONDS,
                )
                await task_service.save_plan(
                    db_task_id, 
                    event.get("data"), 
                    version=new_version,
                    user_feedback=plan_feedback
                )
                await task_service.update_status(db_task_id, "plan_ready", "awaiting_approval")
            elif event.get("type") == "error":
                await _set_task_status(task_id, status="error", error=event.get("data", ""))
                await task_service.update_status(db_task_id, "error", None)
            
            # 推送事件（在状态更新之后）
            await _push_event(task_id, event)
            
            if event.get("type") in ("plan_ready", "error"):
                break

    except Exception as e:
        logger.error(f"[Writing] 重新生成计划失败: {task_id}, {e}")
        await _set_task_status(task_id, status="error", error=str(e))
        await task_service.update_status(db_task_id, "error", None)
    finally:
        _cleanup_task(task_id)
