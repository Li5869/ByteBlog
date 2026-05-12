"""
Nacos 感知的 HTTP 客户端基类
"""

import httpx
from typing import Optional
from loguru import logger
from config.settings import get_settings

BACKEND_SERVICE_NAME = "person-blog"


class NacosAwareClient:
    """Nacos 感知的 HTTP 客户端基类

    封装 Nacos 服务发现逻辑，子类通过继承即可获得 Nacos 能力。
    子类只需定义 service_tag 用于日志标识，以及可选的 timeout 参数。
    """

    def __init__(self, service_tag: str, timeout: httpx.Timeout | float = 30.0):
        settings = get_settings()
        self._service_tag = service_tag
        self._discovered_url: Optional[str] = None
        self.base_url = settings.backend_api_base

        headers = {}
        if settings.backend_api_key:
            headers["X-API-Key"] = settings.backend_api_key

        self.client = httpx.AsyncClient(
            timeout=timeout,
            headers=headers
        )

    async def _discover_backend_url(self) -> Optional[str]:
        """通过 Nacos 服务发现获取 Java 后端地址"""
        try:
            from services.core.nacos_service import get_nacos_service
            nacos_service = get_nacos_service()
            if not nacos_service.is_registered:
                logger.debug(f"[{self._service_tag}] Nacos 未就绪，使用配置的后端地址")
                return None

            url = await nacos_service.get_service_url(BACKEND_SERVICE_NAME)
            if url:
                full_url = f"{url}/api"
                logger.info(f"[{self._service_tag}] 通过 Nacos 发现后端服务: {full_url}")
                return full_url

            logger.warning(f"[{self._service_tag}] Nacos 未找到服务: {BACKEND_SERVICE_NAME}")
            return None
        except Exception as e:
            logger.warning(f"[{self._service_tag}] Nacos 服务发现异常: {e}")
            return None

    async def _get_base_url(self) -> str:
        """获取后端基础 URL（优先 Nacos 发现，回退配置项）"""
        if not self._discovered_url:
            discovered = await self._discover_backend_url()
            if discovered:
                self._discovered_url = discovered
                self.base_url = discovered
        return self.base_url

    async def close(self):
        """关闭 HTTP 客户端"""
        await self.client.aclose()