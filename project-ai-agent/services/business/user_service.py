"""
用户 API 服务
"""

import httpx
from functools import lru_cache
from typing import Optional
from loguru import logger
from models.schemas import UserInfoDTO
from services.business.base_client import NacosAwareClient


@lru_cache
def get_user_service() -> "UserService":
    """获取用户 API 服务单例（@lru_cache 保证单例）"""
    return UserService()


class UserService(NacosAwareClient):
    """用户 API 服务 - 通过 Nacos 发现 Java 后端并获取用户信息"""

    def __init__(self):
        super().__init__(service_tag="UserService", timeout=10.0)

    async def get_user_info(self, user_id: str) -> Optional[UserInfoDTO]:
        """
        获取用户信息

        调用 Java 后端 /ai/internal/user/info 接口获取用户详细信息。
        优先通过 Nacos 发现后端地址，Nacos 不可用时回退到配置项。

        Args:
            user_id: 用户 ID 字符串

        Returns:
            UserInfoDTO 对象，用户不存在或异常时返回 None
        """
        try:
            base_url = await self._get_base_url()
            response = await self.client.get(
                f"{base_url}/ai/internal/user/info",
                params={"userId": user_id}
            )
            response.raise_for_status()
            json_data = response.json()
            user_data = json_data.get("data")

            if not user_data:
                logger.warning(f"[UserService] 用户不存在, userId={user_id}")
                return None

            return UserInfoDTO(**user_data)

        except httpx.HTTPStatusError as e:
            logger.error(f"[UserService] 获取用户信息失败，状态码: {e.response.status_code}")
            return None
        except httpx.RequestError as e:
            logger.error(f"[UserService] 获取用户信息请求异常: {e}")
            return None
        except Exception as e:
            logger.error(f"[UserService] 获取用户信息未知错误: {e}")
            return None
