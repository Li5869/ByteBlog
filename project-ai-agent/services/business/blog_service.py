"""
博客 API 服务
文件：project-ai-agent/services/business/blog_service.py
"""

import httpx
from typing import List
from functools import lru_cache
from loguru import logger
from services.business.base_client import NacosAwareClient


@lru_cache
def get_blog_service() -> "BlogApiService":
    """获取博客 API 服务单例（@lru_cache 保证单例）"""
    return BlogApiService()


class BlogApiService(NacosAwareClient):
    """博客 API 服务 - 通过 Nacos 发现 Java 后端并调用"""

    def __init__(self):
        super().__init__(service_tag="BlogService", timeout=30.0)

    async def get_categories(self) -> List[dict]:
        """
        获取分类列表

        调用 Java 后端 /category 接口获取所有文章分类。
        用于写作 Agent 生成文章时选择分类。

        Returns:
            分类列表，每个分类包含 id、name 等字段
        """
        try:
            response = await self.client.get(f"{await self._get_base_url()}/category")
            response.raise_for_status()
            json_data = response.json()
            return json_data.get("data", [])
        except httpx.HTTPStatusError as e:
            logger.error(f"[BlogService] 获取分类列表失败，状态码: {e.response.status_code}")
            return []
        except httpx.RequestError as e:
            logger.error(f"[BlogService] 获取分类列表请求异常: {e}")
            return []
        except Exception as e:
            logger.error(f"[BlogService] 获取分类列表未知错误: {e}")
            return []

    async def get_tags(self) -> List[dict]:
        """
        获取所有标签列表

        调用 Java 后端 /tag 接口获取所有标签。
        用于写作 Agent 生成文章时选择标签。

        Returns:
            标签列表，每个标签包含 id、name 等字段
        """
        try:
            response = await self.client.get(f"{await self._get_base_url()}/tag")
            response.raise_for_status()
            json_data = response.json()
            return json_data.get("data", [])
        except httpx.HTTPStatusError as e:
            logger.error(f"[BlogService] 获取标签列表失败，状态码: {e.response.status_code}")
            return []
        except httpx.RequestError as e:
            logger.error(f"[BlogService] 获取标签列表请求异常: {e}")
            return []
        except Exception as e:
            logger.error(f"[BlogService] 获取标签列表未知错误: {e}")
            return []

    async def close(self):
        """关闭 HTTP 客户端"""
        await self.client.aclose()
