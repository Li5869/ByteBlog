"""
博主搜索工具（Elasticsearch）
"""

from typing import List, Optional
from langchain_core.tools import tool

from services.store.es_author_service import get_author_es_service


# ==================== LangChain Tool 定义 ====================

@tool
async def search_authors_by_keyword(keyword: str, limit: int = 5) -> List[dict]:
    """
    搜索博主

    根据关键词搜索博客平台的博主，匹配用户名、昵称或个人简介。

    Args:
        keyword: 搜索关键词
        limit: 返回数量限制，默认5条

    Returns:
        匹配的博主列表
    """
    service = get_author_es_service()
    return await service.search_authors(keyword, limit)


@tool
async def get_hot_authors(limit: int = 10) -> List[dict]:
    """
    获取热门博主

    获取粉丝数最多的博主列表。

    Args:
        limit: 返回数量限制，默认10条

    Returns:
        热门博主列表
    """
    service = get_author_es_service()
    return await service.get_hot_authors(limit)


@tool
async def get_author_by_id(author_id: int) -> Optional[dict]:
    """
    获取博主详细信息

    Args:
        author_id: 博主ID

    Returns:
        博主详细信息，不存在返回 None
    """
    service = get_author_es_service()
    return await service.get_author_by_id(author_id)
