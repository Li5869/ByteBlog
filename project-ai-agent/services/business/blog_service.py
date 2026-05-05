"""
博客 API 服务
文件：project-ai-agent/services/business/blog_service.py
"""

import httpx
from typing import Optional, List
from functools import lru_cache
from loguru import logger
from config.settings import get_settings


@lru_cache
def get_blog_service() -> "BlogApiService":
    """获取博客 API 服务单例（@lru_cache 保证单例）"""
    return BlogApiService()


class BlogApiService:
    """博客 API 服务"""

    def __init__(self):
        settings = get_settings()
        self.base_url = settings.backend_api_base

        headers = {}
        if settings.backend_api_key:
            headers["X-API-Key"] = settings.backend_api_key

        self.client = httpx.AsyncClient(
            timeout=30.0,
            headers=headers
        )

    async def get_categories(self) -> List[dict]:
        """
        获取分类列表

        调用 Java 后端 /category 接口获取所有文章分类。
        用于写作 Agent 生成文章时选择分类。

        Returns:
            分类列表，每个分类包含 id、name 等字段
        """
        try:
            response = await self.client.get(f"{self.base_url}/category")
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
            response = await self.client.get(f"{self.base_url}/tag")
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

    async def get_hot_tags(self, limit: int = 20) -> List[dict]:
        """
        获取热门标签

        调用 Java 后端热门标签接口。

        Args:
            limit: 返回数量限制

        Returns:
            热门标签列表
        """
        try:
            response = await self.client.get(
                f"{self.base_url}/article/tags/hot",
                params={"limit": limit}
            )
            response.raise_for_status()
            json_data = response.json()
            return json_data.get("data", [])
        except httpx.HTTPStatusError as e:
            logger.error(f"[BlogService] 获取热门标签失败，状态码: {e.response.status_code}")
            return []
        except httpx.RequestError as e:
            logger.error(f"[BlogService] 获取热门标签请求异常: {e}")
            return []
        except Exception as e:
            logger.error(f"[BlogService] 获取热门标签未知错误: {e}")
            return []

    async def close(self):
        """关闭 HTTP 客户端"""
        await self.client.aclose()
