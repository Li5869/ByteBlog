"""
写作任务服务
文件：project-ai-agent/services/business/writing/writing_task_service.py

调用 Java 后端 API 更新任务状态
"""

import httpx
from typing import Optional, Dict, Any
from functools import lru_cache
from loguru import logger
from services.business.base_client import NacosAwareClient


@lru_cache
def get_writing_task_service() -> "WritingTaskService":
    """获取写作任务服务单例"""
    return WritingTaskService()


class WritingTaskService(NacosAwareClient):
    """写作任务服务 - 通过 Nacos 发现 Java 后端并调用 API"""

    def __init__(self):
        super().__init__(
            service_tag="WritingTask",
            timeout=httpx.Timeout(connect=5.0, read=30.0, write=10.0, pool=5.0)
        )

    async def create_task(self, user_id: Optional[int], user_request: str) -> Optional[int]:
        """
        创建写作任务

        Args:
            user_id: 用户 ID
            user_request: 用户写作需求

        Returns:
            任务 ID
        """
        try:
            response = await self.client.post(
                f"{await self._get_base_url()}/ai/writing/internal/task",
                json={
                    "user_id": user_id,
                    "user_request": user_request
                }
            )
            response.raise_for_status()
            json_data = response.json()
            task_id = json_data.get("data")
            logger.info(f"[WritingTask] 创建任务成功, taskId={task_id}")
            return task_id
        except Exception as e:
            logger.error(f"[WritingTask] 创建任务失败: {e}")
            return None

    async def update_status(self, task_id: int, status: str, current_step: Optional[str] = None) -> bool:
        """
        更新任务状态

        Args:
            task_id: 任务 ID
            status: 任务状态
            current_step: 当前步骤

        Returns:
            是否成功
        """
        try:
            payload = {"status": status}
            if current_step is not None:
                payload["currentStep"] = current_step

            response = await self.client.put(
                f"{await self._get_base_url()}/ai/writing/internal/task/{task_id}/status",
                json=payload
            )
            response.raise_for_status()
            logger.info(f"[WritingTask] 更新状态成功, taskId={task_id}, status={status}, step={current_step}")
            return True
        except httpx.HTTPStatusError as e:
            logger.error(f"[WritingTask] 更新状态失败: HTTP {e.response.status_code} - {e.response.text}")
            return False
        except Exception as e:
            logger.error(f"[WritingTask] 更新状态失败: {type(e).__name__}: {e}")
            return False

    async def update_revision_count(self, task_id: int, revision_count: int) -> bool:
        """
        更新修订次数

        Args:
            task_id: 任务 ID
            revision_count: 修订次数

        Returns:
            是否成功
        """
        try:
            response = await self.client.put(
                f"{await self._get_base_url()}/ai/writing/internal/task/{task_id}/revision",
                params={"revisionCount": revision_count}
            )
            response.raise_for_status()
            logger.info(f"[WritingTask] 更新修订次数成功, taskId={task_id}, count={revision_count}")
            return True
        except Exception as e:
            logger.error(f"[WritingTask] 更新修订次数失败: {e}")
            return False

    async def complete_task(self, task_id: int, final_action: Optional[str] = None) -> bool:
        """
        完成任务

        Args:
            task_id: 任务 ID
            final_action: 最终动作

        Returns:
            是否成功
        """
        try:
            params = {}
            if final_action:
                params["finalAction"] = final_action

            response = await self.client.put(
                f"{await self._get_base_url()}/ai/writing/internal/task/{task_id}/complete",
                params=params
            )
            response.raise_for_status()
            logger.info(f"[WritingTask] 完成任务成功, taskId={task_id}")
            return True
        except Exception as e:
            logger.error(f"[WritingTask] 完成任务失败: {e}")
            return False

    async def save_plan(self, task_id: int, plan_data: Dict[str, Any],
                       version: int = 1, user_feedback: Optional[str] = None) -> Optional[int]:
        """
        保存写作计划

        Args:
            task_id: 任务 ID
            plan_data: 计划数据
            version: 版本号
            user_feedback: 用户反馈

        Returns:
            计划 ID
        """
        try:
            response = await self.client.post(
                f"{await self._get_base_url()}/ai/writing/internal/plan",
                json={
                    "task_id": task_id,
                    "plan_data": plan_data,
                    "version": version,
                    "user_feedback": user_feedback
                }
            )
            response.raise_for_status()
            json_data = response.json()
            plan_id = json_data.get("data")
            logger.info(f"[WritingTask] 保存计划成功, planId={plan_id}, taskId={task_id}")
            return plan_id
        except Exception as e:
            logger.error(f"[WritingTask] 保存计划失败: {e}")
            return None

    async def update_plan_approval_status(self, plan_id: int, approval_status: str) -> bool:
        """
        更新计划审核状态

        Args:
            plan_id: 计划 ID
            approval_status: 审核状态 (pending/approved/rejected)

        Returns:
            是否成功
        """
        try:
            response = await self.client.put(
                f"{await self._get_base_url()}/ai/writing/internal/plan/{plan_id}/approval",
                params={"approvalStatus": approval_status}
            )
            response.raise_for_status()
            logger.info(f"[WritingTask] 更新计划审核状态成功, planId={plan_id}, status={approval_status}")
            return True
        except Exception as e:
            logger.error(f"[WritingTask] 更新计划审核状态失败: {e}")
            return False

    async def get_task(self, task_id: int) -> Optional[Dict[str, Any]]:
        """
        获取任务信息

        Args:
            task_id: 任务 ID

        Returns:
            任务信息
        """
        try:
            response = await self.client.get(
                f"{await self._get_base_url()}/ai/writing/internal/task/{task_id}"
            )
            response.raise_for_status()
            json_data = response.json()
            return json_data.get("data")
        except Exception as e:
            logger.error(f"[WritingTask] 获取任务失败: {e}")
            return None

    async def get_latest_plan(self, task_id: int) -> Optional[Dict[str, Any]]:
        """
        获取最新计划

        Args:
            task_id: 任务 ID

        Returns:
            计划信息
        """
        try:
            response = await self.client.get(
                f"{await self._get_base_url()}/ai/writing/internal/plan/latest/{task_id}"
            )
            response.raise_for_status()
            json_data = response.json()
            return json_data.get("data")
        except Exception as e:
            logger.error(f"[WritingTask] 获取最新计划失败: {e}")
            return None

    # ==================== 草稿相关方法 ====================

    async def save_draft(self, task_id: int, user_id: Optional[int], draft_data: Dict[str, Any]) -> Optional[int]:
        """
        保存写作草稿

        Args:
            task_id: 任务 ID
            user_id: 用户 ID
            draft_data: 草稿数据

        Returns:
            草稿 ID
        """
        try:
            response = await self.client.post(
                f"{await self._get_base_url()}/ai/writing/internal/draft",
                json={
                    "task_id": task_id,
                    "user_id": user_id,
                    "draft_data": draft_data
                }
            )
            response.raise_for_status()
            json_data = response.json()
            draft_id = json_data.get("data")
            logger.info(f"[WritingTask] 保存草稿成功, draftId={draft_id}, taskId={task_id}")
            return draft_id
        except Exception as e:
            logger.error(f"[WritingTask] 保存草稿失败: {e}")
            return None

    async def close(self):
        """关闭 HTTP 客户端"""
        await self.client.aclose()
