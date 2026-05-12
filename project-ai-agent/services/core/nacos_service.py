"""
Nacos 服务注册模块（适配 nacos-sdk-python 2.x，同步 HTTP API）
文件：project-ai-agent/services/core/nacos_service.py
"""

import asyncio
import socket
from typing import Optional

import nacos
from loguru import logger

from config.settings import get_settings


class NacosService:
    """Nacos 服务注册管理（nacos-sdk-python 2.x）"""

    def __init__(self):
        self._client: Optional[nacos.NacosClient] = None
        self._registered = False
        self._service_name: str = ""
        self._ip: str = ""
        self._port: int = 0
        self._running = False
        self._heartbeat_task: Optional[asyncio.Task] = None

    @property
    def is_registered(self) -> bool:
        return self._registered

    def _get_local_ip(self) -> str:
        """获取本机 IP 地址"""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return "127.0.0.1"

    def _create_client(self) -> nacos.NacosClient:
        """创建 Nacos 客户端（同步）"""
        settings = get_settings()
        return nacos.NacosClient(
            server_addresses=settings.nacos_server_addr,
            namespace=settings.nacos_namespace,
            username=settings.nacos_username or None,
            password=settings.nacos_password or None,
        )

    def _register_sync(self, quiet: bool = False) -> bool:
        """同步注册服务到 Nacos"""
        settings = get_settings()

        if not settings.nacos_enabled:
            logger.info("[Nacos] 服务注册已禁用，跳过注册")
            return False

        try:
            self._service_name = settings.nacos_service_name
            self._ip = self._get_local_ip()
            self._port = settings.port

            self._client = self._create_client()

            self._client.add_naming_instance(
                self._service_name,
                self._ip,
                self._port,
                group_name=settings.nacos_group,
                weight=1.0,
                ephemeral=True,
                metadata={
                    "version": "1.0.0",
                    "preserved.register.source": "PYTHON",
                },
            )

            self._registered = True
            if not quiet:
                logger.info(
                    f"[Nacos] 服务注册成功: {self._service_name} -> {self._ip}:{self._port}"
                )
            return True

        except Exception as e:
            logger.error(f"[Nacos] 服务注册失败: {e}")
            return False

    async def _heartbeat_loop(self):
        """心跳守护：每 5 秒重新注册，弥补 SDK 心跳在鉴权模式下失效的问题"""
        while self._running:
            await asyncio.sleep(5)
            try:
                await asyncio.to_thread(self._register_sync, True)
            except Exception as e:
                logger.warning(f"[Nacos] 心跳重注册异常: {e}")

    def _deregister_sync(self) -> bool:
        """同步注销服务"""
        if not self._registered or not self._client:
            return True

        settings = get_settings()

        try:
            self._client.remove_naming_instance(
                self._service_name,
                self._ip,
                self._port,
                group_name=settings.nacos_group,
                ephemeral=True,
            )

            self._registered = False
            return True

        except Exception as e:
            logger.error(f"[Nacos] 服务注销失败: {e}")
            return False

    def _get_service_url_sync(self, service_name: str) -> Optional[str]:
        """同步获取指定服务的 URL"""
        if not self._client:
            logger.warning("[Nacos] 客户端未初始化，无法进行服务发现")
            return None

        settings = get_settings()

        try:
            result = self._client.list_naming_instance(
                service_name,
                group_name=settings.nacos_group,
                healthy_only=True,
            )

            hosts = result.get("hosts", [])
            if not hosts:
                logger.warning(f"[Nacos] 未找到健康的服务实例: {service_name}")
                return None

            host = hosts[0]
            return f"http://{host['ip']}:{host['port']}"

        except Exception as e:
            logger.error(f"[Nacos] 服务发现失败: {e}")
            return None

    def _shutdown_sync(self):
        """同步关闭客户端（2.x SDK 无需显式关闭）"""
        self._client = None
        logger.info("[Nacos] 客户端已关闭")

    # ==================== 异步包装（兼容上层调用） ====================

    async def register(self) -> bool:
        """注册服务到 Nacos，并启动心跳守护"""
        self._running = True
        result = await asyncio.to_thread(self._register_sync)
        if result:
            self._heartbeat_task = asyncio.create_task(self._heartbeat_loop())
        return result

    async def deregister(self) -> bool:
        """从 Nacos 注销服务并停止心跳"""
        self._running = False
        if self._heartbeat_task:
            self._heartbeat_task.cancel()
            self._heartbeat_task = None
        return await asyncio.to_thread(self._deregister_sync)

    async def get_service_url(self, service_name: str) -> Optional[str]:
        """获取指定服务的 URL（用于服务发现）"""
        return await asyncio.to_thread(self._get_service_url_sync, service_name)

    async def shutdown(self):
        """关闭 Nacos 客户端"""
        self._running = False
        if self._heartbeat_task:
            self._heartbeat_task.cancel()
            self._heartbeat_task = None
        await asyncio.to_thread(self._shutdown_sync)


_nacos_service: Optional[NacosService] = None


def get_nacos_service() -> NacosService:
    """获取 Nacos 服务单例"""
    global _nacos_service
    if _nacos_service is None:
        _nacos_service = NacosService()
    return _nacos_service
