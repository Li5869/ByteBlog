"""
博主 ES 服务

继承 ElasticsearchBaseService，只定义博主特有的查询逻辑和字段映射。
"""

from typing import List, Optional
from loguru import logger

from common.elasticsearch_base import ElasticsearchBaseService
from common.constants import ES_INDEX_AUTHOR, ES_DEFAULT_SEARCH_SIZE


class AuthorESService(ElasticsearchBaseService):
    """博主 ES 服务"""

    index_name: str = ES_INDEX_AUTHOR

    # ==================== 字段映射 ====================

    def _map_hit(self, hit: dict) -> dict:
        """将 ES hit 映射为博主业务 dict"""
        source = hit["_source"]
        return {
            "id": source.get("id"),
            "username": source.get("username", ""),
            "nickname": source.get("nickname", ""),
            "avatar": source.get("avatar", ""),
            "bio": source.get("bio", ""),
            "articles_count": source.get("articlesCount", 0),
            "fans_count": source.get("fansCount", 0),
            "likes_count": source.get("likesCount", 0),
            "score": hit.get("_score", 0),
        }

    def _map_source(self, source: dict) -> dict:
        """博主详情映射"""
        return {
            "id": source.get("id"),
            "username": source.get("username", ""),
            "nickname": source.get("nickname", ""),
            "avatar": source.get("avatar", ""),
            "bio": source.get("bio", ""),
            "articles_count": source.get("articlesCount", 0),
            "fans_count": source.get("fansCount", 0),
            "likes_count": source.get("likesCount", 0),
        }

    # ==================== 业务查询方法 ====================

    async def search_authors(self, keyword: str, size: int = ES_DEFAULT_SEARCH_SIZE) -> List[dict]:
        """搜索博主（昵称 + 简介 + 用户名匹配）"""
        return await self.search(
            query={
                "bool": {
                    "should": [
                        {"match": {"nickname": {"query": keyword, "boost": 2.0}}},
                        {"match": {"bio": keyword}},
                        {"wildcard": {"username": f"*{keyword}*"}},
                    ],
                    "filter": [{"term": {"status": 1}}],
                }
            },
            sort=[
                {"fansCount": {"order": "desc"}},
                {"articlesCount": {"order": "desc"}},
            ],
            source=["id", "username", "nickname", "avatar", "bio",
                     "articlesCount", "fansCount", "likesCount"],
            size=size,
        )

    async def get_hot_authors(self, size: int = 10) -> List[dict]:
        """获取热门博主（粉丝数排序，仅活跃用户）"""
        return await self.search(
            query={
                "bool": {
                    "filter": [
                        {"term": {"status": 1}},
                        {"range": {"articlesCount": {"gt": 0}}},
                    ]
                }
            },
            sort=[
                {"fansCount": {"order": "desc"}},
                {"articlesCount": {"order": "desc"}},
            ],
            source=["id", "username", "nickname", "avatar", "bio",
                     "articlesCount", "fansCount", "likesCount"],
            size=size,
        )

    async def get_author_by_id(self, author_id: int) -> Optional[dict]:
        """根据 ID 获取博主信息"""
        return await self.get_by_id(str(author_id))


# ==================== 单例工厂 ====================

_author_es_service: Optional[AuthorESService] = None


def get_author_es_service() -> AuthorESService:
    """获取博主 ES 服务单例"""
    global _author_es_service
    if _author_es_service is None:
        _author_es_service = AuthorESService()
    return _author_es_service
