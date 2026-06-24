"""
研究报告持久化服务

通过 Nacos 感知的 HTTP 客户端调用 Java 后端接口，持久化研究报告。
与 BlogApiService、UserService 等保持一致的通信方式。
"""

import httpx
from typing import Optional
from functools import lru_cache
from loguru import logger
from services.business.base_client import NacosAwareClient


@lru_cache
def get_research_service() -> "ResearchApiService":
    """获取研究报告服务单例（@lru_cache 保证单例）"""
    return ResearchApiService()


class ResearchApiService(NacosAwareClient):
    """研究报告 API 服务 - 通过 Nacos 发现 Java 后端并调用"""

    def __init__(self):
        super().__init__(service_tag="ResearchService", timeout=30.0)

    async def persist_report(
        self,
        task_id: str,
        content: str,
        summary: str,
        key_findings: list[str],
        sources: list[dict],
    ) -> Optional[str]:
        """
        持久化研究报告到 Java 后端

        Args:
            task_id: 研究任务 ID
            content: 报告正文
            summary: 报告摘要
            key_findings: 关键发现列表
            sources: 参考来源列表

        Returns:
            报告 URL，失败返回 None
        """
        try:
            response = await self.client.post(
                f"{await self._get_base_url()}/ai/research/internal/report",
                json={
                    "taskId": task_id,
                    "content": content,
                    "summary": summary,
                    "keyFindings": key_findings,
                    "sources": sources,
                },
            )
            response.raise_for_status()
            json_data = response.json()

            code = json_data.get("code", -1)
            if code != 0:
                msg = json_data.get("msg", "未知错误")
                logger.error(f"[ResearchService] 持久化报告业务失败, code={code}, msg={msg}, taskId={task_id}")
                return None

            report_url = json_data.get("data", "")
            logger.info(f"[ResearchService] 报告持久化成功, taskId={task_id}, url={report_url}")
            return report_url
        except httpx.HTTPStatusError as e:
            logger.error(f"[ResearchService] 持久化报告HTTP错误, 状态码: {e.response.status_code}, taskId={task_id}")
            return None
        except httpx.RequestError as e:
            logger.error(f"[ResearchService] 持久化报告请求异常: {e}, taskId={task_id}")
            return None
        except Exception as e:
            logger.error(f"[ResearchService] 持久化报告未知错误: {e}, taskId={task_id}")
            return None
