"""
写作执行服务
文件：project-ai-agent/services/business/writing/writing_execution_service.py

封装写作任务的执行逻辑，供 API 路由和工具调用。
"""

import asyncio
import json
from typing import Dict, Optional

from loguru import logger

from services.core.memory_service import get_memory_service
from services.business.writing.writing_task_service import get_writing_task_service
from agents.writing_agent import get_writing_agent
from common.constants import (
    REDIS_KEY_WRITING_TASK_PREFIX,
    REDIS_KEY_WRITING_EVENTS_PREFIX,
    REDIS_KEY_WRITING_PLAN_PREFIX,
    WRITING_TASK_TTL_SECONDS,
)

_running_tasks: Dict[str, asyncio.Task] = {}


def _task_key(task_id: str) -> str:
    return f"{REDIS_KEY_WRITING_TASK_PREFIX}{task_id}"


def _events_key(task_id: str) -> str:
    return f"{REDIS_KEY_WRITING_EVENTS_PREFIX}{task_id}"


def _plan_key(task_id: str) -> str:
    return f"{REDIS_KEY_WRITING_PLAN_PREFIX}{task_id}"


async def _get_redis():
    return await get_memory_service()._get_client()


async def _push_event(task_id: str, event: dict):
    redis = await _get_redis()
    await redis.xadd(_events_key(task_id), {"event": json.dumps(event, ensure_ascii=False)})
    await redis.expire(_events_key(task_id), WRITING_TASK_TTL_SECONDS)


async def _set_task_status(task_id: str, **fields):
    redis = await _get_redis()
    await redis.hset(_task_key(task_id), mapping=fields)
    await redis.expire(_task_key(task_id), WRITING_TASK_TTL_SECONDS)


def _cleanup_task(task_id: str):
    if task_id in _running_tasks:
        del _running_tasks[task_id]


async def cancel_task(task_id: str, reason: str = "用户取消") -> bool:
    """
    取消写作任务
    
    Args:
        task_id: 写作任务ID
        reason: 取消原因
        
    Returns:
        是否成功取消
    """
    try:
        redis = await _get_redis()
        
        if not await redis.exists(_task_key(task_id)):
            logger.warning(f"[WritingExecution] 任务不存在, taskId={task_id}")
            return False
        
        await _set_task_status(task_id, status="cancelled", error=reason)
        await redis.delete(_events_key(task_id))
        
        if task_id in _running_tasks:
            _running_tasks[task_id].cancel()
            _cleanup_task(task_id)
        
        logger.info(f"[WritingExecution] 取消任务: {task_id}")
        return True
        
    except Exception as e:
        logger.error(f"[WritingExecution] 取消任务失败: {e}")
        return False


async def stop_task(task_id: str, save_partial: bool = False) -> bool:
    """
    停止写作任务
    
    Args:
        task_id: 写作任务ID
        save_partial: 是否保存部分进度
        
    Returns:
        是否成功停止
    """
    try:
        redis = await _get_redis()
        
        if not await redis.exists(_task_key(task_id)):
            logger.warning(f"[WritingExecution] 任务不存在, taskId={task_id}")
            return False
        
        await _set_task_status(task_id, status="stopped")
        
        if task_id in _running_tasks:
            _running_tasks[task_id].cancel()
            _cleanup_task(task_id)
        
        if not save_partial:
            await redis.delete(_events_key(task_id))
        
        logger.info(f"[WritingExecution] 停止任务: {task_id}")
        return True
        
    except Exception as e:
        logger.error(f"[WritingExecution] 停止任务失败: {e}")
        return False


async def finalize_task(task_id: str) -> bool:
    """
    完成写作任务
    
    Args:
        task_id: 写作任务ID
        
    Returns:
        是否成功完成
    """
    try:
        redis = await _get_redis()
        
        if not await redis.exists(_task_key(task_id)):
            logger.warning(f"[WritingExecution] 任务不存在, taskId={task_id}")
            return False
        
        await _set_task_status(task_id, status="finalized")
        await redis.delete(_events_key(task_id))
        await redis.delete(_task_key(task_id))
        
        logger.info(f"[WritingExecution] 完成任务: {task_id}")
        return True
        
    except Exception as e:
        logger.error(f"[WritingExecution] 完成任务失败: {e}")
        return False


async def start_execute_phase(task_id: str) -> bool:
    """
    启动写作执行阶段
    
    Args:
        task_id: 写作任务ID
        
    Returns:
        是否成功启动
    """
    try:
        redis = await _get_redis()
        
        plan_json = await redis.get(_plan_key(task_id))
        if not plan_json:
            logger.error(f"[WritingExecution] 计划不存在, taskId={task_id}")
            return False

        await redis.delete(_events_key(task_id))
        await _set_task_status(task_id, status="executing", step="title")

        task_service = get_writing_task_service()
        db_task_id = int(task_id)
        latest_plan = await task_service.get_latest_plan(db_task_id)
        if latest_plan and latest_plan.get("id"):
            await task_service.update_plan_approval_status(latest_plan["id"], "approved")

        task = asyncio.create_task(_run_execute_phase(task_id))
        _running_tasks[task_id] = task

        logger.info(f"[WritingExecution] 启动执行阶段, taskId={task_id}")
        return True

    except Exception as e:
        logger.error(f"[WritingExecution] 启动执行失败: {e}")
        return False


async def _run_execute_phase(task_id: str):
    """执行写作流程（标题→摘要→内容→标签→反思→完成）"""
    try:
        agent = get_writing_agent()
        task_service = get_writing_task_service()

        db_task_id = int(task_id)
        await task_service.update_status(db_task_id, "executing", "title")

        async def progress_handler(event):
            await _push_event(task_id, event)
            if event.get("type") == "phase":
                phase_data = event.get("data", {})
                step = phase_data.get("step")
                if step:
                    await task_service.update_status(db_task_id, "executing", step)

        agent.progress_callback = progress_handler

        async for event in agent.execute_stream(thread_id=task_id):
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
                await _set_task_status(task_id, status="finalized")

            elif event.get("type") == "error":
                await _set_task_status(task_id, status="error", error=event.get("data", ""))
                await task_service.update_status(db_task_id, "error", None)
            
            await _push_event(task_id, event)
            
            if event.get("type") in ("done", "error"):
                break

    except asyncio.CancelledError:
        await _set_task_status(task_id, status="stopped")
        await task_service.update_status(db_task_id, "stopped", None)
    except Exception as e:
        logger.error(f"[WritingExecution] 执行异常: {task_id}, {e}")
        await _set_task_status(task_id, status="error", error=str(e))
        await task_service.update_status(db_task_id, "error", None)
    finally:
        agent.progress_callback = None
        _cleanup_task(task_id)


async def start_plan_phase(task_id: str, user_request: str) -> bool:
    """
    启动写作计划生成阶段
    
    Args:
        task_id: 写作任务ID
        user_request: 用户写作需求
        
    Returns:
        是否成功启动
    """
    try:
        redis = await _get_redis()
        
        await redis.delete(_events_key(task_id))
        await _set_task_status(task_id, status="planning", step="generating_plan")
        await redis.set(_plan_key(task_id), "", ex=WRITING_TASK_TTL_SECONDS)

        task = asyncio.create_task(_run_plan_phase(task_id, user_request))
        _running_tasks[task_id] = task

        logger.info(f"[WritingExecution] 启动计划阶段, taskId={task_id}")
        return True

    except Exception as e:
        logger.error(f"[WritingExecution] 启动计划失败: {e}")
        return False


async def _run_plan_phase(task_id: str, user_request: str):
    """生成写作计划"""
    try:
        agent = get_writing_agent()
        task_service = get_writing_task_service()
        redis = await _get_redis()

        db_task_id = int(task_id)
        await task_service.update_status(db_task_id, "planning", "generating_plan")

        initial_state = {
            "user_request": user_request,
            "plan": None, "plan_approved": False, "plan_feedback": None,
            "current_step": None, "writing_result": None, "references": None,
            "reflection": None, "revision_count": 0, "article_id": None,
            "action": None, "cover": None, "error": None,
        }

        plan_result = None
        
        async for event in agent.generate_plan(initial_state, thread_id=task_id):
            if event.get("type") == "plan_ready":
                plan_result = event.get("data")
                
                await redis.set(_plan_key(task_id), json.dumps(plan_result, ensure_ascii=False), ex=WRITING_TASK_TTL_SECONDS)
                await task_service.save_plan(db_task_id, plan_result, version=1)
                await task_service.update_status(db_task_id, "plan_ready", "awaiting_approval")
                await _set_task_status(task_id, status="plan_ready", step="awaiting_approval")
                
            elif event.get("type") == "error":
                await _set_task_status(task_id, status="error", error=event.get("data", ""))
                await task_service.update_status(db_task_id, "error", None)
            
            await _push_event(task_id, event)

        if plan_result:
            logger.info(f"[WritingExecution] 计划生成完成, taskId={task_id}")

    except asyncio.CancelledError:
        await _set_task_status(task_id, status="stopped")
        await task_service.update_status(db_task_id, "stopped", None)
    except Exception as e:
        logger.error(f"[WritingExecution] 计划生成异常: {task_id}, {e}")
        await _set_task_status(task_id, status="error", error=str(e))
        await task_service.update_status(db_task_id, "error", None)
    finally:
        _cleanup_task(task_id)


async def handle_approve(task_id: str) -> dict:
    """
    处理用户批准计划
    
    Args:
        task_id: 写作任务ID
        
    Returns:
        包含 success, status, error 的结果字典
    """
    try:
        redis = await _get_redis()
        
        plan_json = await redis.get(_plan_key(task_id))
        if not plan_json:
            return {"success": False, "status": None, "error": "计划不存在，请重新开始"}
        
        success = await start_execute_phase(task_id)
        if not success:
            return {"success": False, "status": None, "error": "启动执行阶段失败"}
        
        logger.info(f"[WritingExecution] 用户批准计划，开始执行: {task_id}")
        return {"success": True, "status": "executing", "error": None}
        
    except Exception as e:
        logger.error(f"[WritingExecution] 处理批准失败: {e}")
        return {"success": False, "status": None, "error": str(e)}


async def handle_revise(task_id: str, feedback: str) -> dict:
    """
    处理用户修改计划
    
    Args:
        task_id: 写作任务ID
        feedback: 用户修改意见
        
    Returns:
        包含 success, status, error 的结果字典
    """
    try:
        if not feedback:
            return {"success": False, "status": None, "error": "修改意见不能为空"}
        
        success = await start_revise_phase(task_id, feedback)
        if not success:
            return {"success": False, "status": None, "error": "启动计划修改失败"}
        
        logger.info(f"[WritingExecution] 用户要求修改计划: {task_id}")
        return {"success": True, "status": "planning", "error": None}
        
    except Exception as e:
        logger.error(f"[WritingExecution] 处理修改失败: {e}")
        return {"success": False, "status": None, "error": str(e)}


async def start_revise_phase(task_id: str, feedback: str) -> bool:
    """
    启动计划修改阶段
    
    Args:
        task_id: 写作任务ID
        feedback: 用户修改意见
        
    Returns:
        是否成功启动
    """
    try:
        redis = await _get_redis()
        task_service = get_writing_task_service()
        
        db_task_id = int(task_id)
        await task_service.update_status(db_task_id, "planning", "regenerating_plan")
        
        latest_plan = await task_service.get_latest_plan(db_task_id)
        if latest_plan and latest_plan.get("id"):
            await task_service.update_plan_approval_status(latest_plan["id"], "rejected")

        user_request = await redis.hget(_task_key(task_id), "user_request")

        await redis.delete(_events_key(task_id))
        await _set_task_status(task_id, status="planning", step="regenerating_plan")

        task = asyncio.create_task(_run_revise_phase(task_id, user_request or "", feedback))
        _running_tasks[task_id] = task

        logger.info(f"[WritingExecution] 启动修改阶段, taskId={task_id}")
        return True

    except Exception as e:
        logger.error(f"[WritingExecution] 启动修改失败: {e}")
        return False


async def _run_revise_phase(task_id: str, user_request: str, feedback: str):
    """修改写作计划"""
    try:
        agent = get_writing_agent()
        task_service = get_writing_task_service()
        redis = await _get_redis()

        db_task_id = int(task_id)
        
        old_plan_json = await redis.get(_plan_key(task_id))
        old_plan_dict = json.loads(old_plan_json) if old_plan_json else None
        
        from models.writing_models import WritingPlan
        old_plan = WritingPlan(**old_plan_dict) if old_plan_dict else None

        latest_plan = await task_service.get_latest_plan(db_task_id)
        new_version = (latest_plan.get("version", 0) + 1) if latest_plan else 2

        initial_state = {
            "user_request": user_request,
            "plan": old_plan, "plan_approved": False, "plan_feedback": feedback,
            "current_step": None, "writing_result": None, "references": None,
            "reflection": None, "revision_count": 0, "article_id": None,
            "action": None, "cover": None, "error": None,
        }

        plan_result = None
        
        async for event in agent.generate_plan(initial_state, thread_id=task_id):
            if event.get("type") == "plan_ready":
                plan_result = event.get("data")
                
                await redis.set(_plan_key(task_id), json.dumps(plan_result, ensure_ascii=False), ex=WRITING_TASK_TTL_SECONDS)
                await task_service.save_plan(db_task_id, plan_result, version=new_version, user_feedback=feedback)
                await task_service.update_status(db_task_id, "plan_ready", "awaiting_approval")
                await _set_task_status(task_id, status="plan_ready", step="awaiting_approval")
                
            elif event.get("type") == "error":
                await _set_task_status(task_id, status="error", error=event.get("data", ""))
                await task_service.update_status(db_task_id, "error", None)
            
            await _push_event(task_id, event)

        if plan_result:
            logger.info(f"[WritingExecution] 计划修改完成, taskId={task_id}")

    except asyncio.CancelledError:
        await _set_task_status(task_id, status="stopped")
        await task_service.update_status(db_task_id, "stopped", None)
    except Exception as e:
        logger.error(f"[WritingExecution] 计划修改异常: {task_id}, {e}")
        await _set_task_status(task_id, status="error", error=str(e))
        await task_service.update_status(db_task_id, "error", None)
    finally:
        _cleanup_task(task_id)
