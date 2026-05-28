"""
博客 API 服务
文件：project-ai-agent/services/business/blog_service.py
"""

import httpx
from typing import List, Optional
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

    async def publish_article(self, user_id: int, article_data: dict) -> Optional[dict]:
        """
        发布或保存文章草稿

        调用 Java 后端 /ai/article/internal/create 接口发布文章。
        用于 SmartAgent 调用发布写作任务生成的文章。

        Args:
            user_id: 用户 ID
            article_data: 文章数据，包含 title, summary, content, cover, categoryId, tagIds, tagNames, status, taskId

        Returns:
            发布结果，包含文章 ID 和状态
        """
        try:
            response = await self.client.post(
                f"{await self._get_base_url()}/ai/article/internal/create",
                params={"userId": user_id},
                json=article_data
            )
            response.raise_for_status()
            json_data = response.json()

            code = json_data.get("code", -1)
            if code != 0:
                msg = json_data.get("msg", "未知错误")
                logger.error(f"[BlogService] 发布文章业务失败, code={code}, msg={msg}, userId={user_id}")
                return None

            result = json_data.get("data")
            logger.info(f"[BlogService] 发布文章成功, userId={user_id}, articleId={result.get('id') if result else None}")
            return result
        except httpx.HTTPStatusError as e:
            logger.error(f"[BlogService] 发布文章失败，状态码: {e.response.status_code}, 响应: {e.response.text}")
            return None
        except httpx.RequestError as e:
            logger.error(f"[BlogService] 发布文章请求异常: {e}")
            return None
        except Exception as e:
            logger.error(f"[BlogService] 发布文章未知错误: {e}")
            return None

    async def close(self):
        """关闭 HTTP 客户端"""
        await self.client.aclose()
