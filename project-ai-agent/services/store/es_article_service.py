"""
文章 ES 服务

继承 ElasticsearchBaseService，只定义文章特有的查询逻辑和字段映射。
"""

from typing import List, Optional

from common.constants import ES_INDEX_ARTICLE, ES_DEFAULT_SEARCH_SIZE
from common.elasticsearch_base import ElasticsearchBaseService


class ArticleESService(ElasticsearchBaseService):
    """文章 ES 服务"""

    index_name: str = ES_INDEX_ARTICLE

    # ==================== 字段映射（类似 Java 的 ResultMapper） ====================

    def _map_hit(self, hit: dict) -> dict:
        """将 ES hit 映射为文章业务 dict"""
        source = hit["_source"]
        return {
            "id": source.get("id"),
            "title": source.get("title", ""),
            "summary": source.get("summary", ""),
            "author_name": source.get("authorName", ""),
            "category_name": source.get("categoryName", ""),
            "views": source.get("views", 0),
            "likes": source.get("likes", 0),
            "score": hit.get("_score", 0),
        }

    def _map_source(self, source: dict) -> dict:
        """文章详情映射（比搜索结果更完整）"""
        return {
            "id": source.get("id"),
            "title": source.get("title", ""),
            "summary": source.get("summary", ""),
            "author_name": source.get("authorName", ""),
            "author_avatar": source.get("authorAvatar", ""),
            "category_id": source.get("categoryId"),
            "category_name": source.get("categoryName", ""),
            "tags": source.get("tags", []),
            "views": source.get("views", 0),
            "likes": source.get("likes", 0),
            "comments": source.get("comments", 0),
            "collections": source.get("collections", 0),
            "cover": source.get("cover", ""),
            "created_at": source.get("createdAt", ""),
        }

    # ==================== 业务查询方法 ====================

    async def search_articles(self, keyword: str, size: int = ES_DEFAULT_SEARCH_SIZE) -> List[dict]:
        """搜索文章（标题 + 摘要匹配，仅已发布）"""
        return await self.search(
            query={
                "bool": {
                    "should": [
                        {"match": {"title": {"query": keyword, "boost": 2.0}}},
                        {"match": {"summary": keyword}},
                    ],
                    "filter": [{"term": {"status": 1}}],
                }
            },
            source=["id", "title", "summary", "authorName", "views", "likes",
                     "categoryId", "categoryName", "tags", "cover", "createdAt"],
            size=size,
        )

    async def get_hot_articles(self, size: int = 10) -> List[dict]:
        """获取热门文章（浏览量排序，仅已发布+热门）"""
        return await self.search(
            query={
                "bool": {
                    "filter": [
                        {"term": {"status": 1}},
                        {"term": {"isHot": True}},
                    ]
                }
            },
            sort=[{"views": {"order": "desc"}}],
            source=["id", "title", "summary", "authorName", "views", "likes",
                     "categoryId", "categoryName", "cover", "createdAt"],
            size=size,
        )

    async def get_article_by_id(self, article_id: int) -> Optional[dict]:
        """根据 ID 获取文章详情"""
        return await self.get_by_id(str(article_id))


# ==================== 单例工厂（类似 Java 的 @Component + @Lazy） ====================

_article_es_service: Optional[ArticleESService] = None


def get_article_es_service() -> ArticleESService:
    """获取文章 ES 服务单例"""
    global _article_es_service
    if _article_es_service is None:
        _article_es_service = ArticleESService()
    return _article_es_service
