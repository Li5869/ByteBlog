"""
文章搜索工具（Elasticsearch）
"""

from typing import List, Optional
from loguru import logger
from langchain_core.tools import tool
from langchain_tavily import TavilySearch

from config.settings import get_settings
from services.store.es_article_service import get_article_es_service
from services.business.blog_service import get_blog_service


# ==================== 外部搜索：Tavily ====================

def _create_tavily_tool() -> Optional[TavilySearch]:
    """创建 Tavily 外部搜索工具（懒加载）"""
    api_key = get_settings().tavily_api_key
    if not api_key:
        logger.warning("TAVILY_API_KEY 未设置，外部搜索功能将不可用")
        return None

    return TavilySearch(
        tavily_api_key=api_key,
        max_results=3,
        search_depth="basic",
        topic="general",
        include_answer=False,
        include_raw_content=False,
        include_domains=[
            "csdn.net", "cnblogs.com", "juejin.cn", "segmentfault.com",
            "oschina.net", "zhihu.com", "infoq.cn", "51cto.com",
            "tech.meituan.com", "developer.aliyun.com", "cloud.tencent.com",
            "liaoxuefeng.com",
        ],
        exclude_domains=[],
        time_range=None,
        auto_parameters=False,
    )


# ==================== LangChain Tool 定义 ====================

@tool
async def search_articles_by_keyword(keyword: str, limit: int = 5) -> List[dict]:
    """
    搜索博客文章

    根据关键词在 Elasticsearch 中搜索博客文章，返回匹配的文章列表。

    Args:
        keyword: 搜索关键词
        limit: 返回数量限制，默认5条

    Returns:
        匹配的文章列表
    """
    service = get_article_es_service()
    return await service.search_articles(keyword, limit)


@tool
async def get_hot_articles(limit: int = 10) -> List[dict]:
    """
    获取热门文章

    获取浏览量最高的文章列表。

    Args:
        limit: 返回数量限制，默认10条

    Returns:
        热门文章列表
    """
    service = get_article_es_service()
    return await service.get_hot_articles(limit)


@tool
async def get_article_by_id(article_id: int) -> Optional[dict]:
    """
    获取文章基本信息

    Args:
        article_id: 文章ID

    Returns:
        文章详情，不存在返回 None
    """
    service = get_article_es_service()
    return await service.get_article_by_id(article_id)


@tool
async def get_article_content_by_id(article_id: int) -> Optional[str]:
    """
    获取文章正文内容

    根据文章ID通过 Java 后端 API 获取文章的完整 Markdown 正文。
    复用 BlogApiService，通过 Nacos 服务发现调用后端。

    Args:
        article_id: 文章ID

    Returns:
        文章内容，不存在返回 None
    """
    blog_service = get_blog_service()
    return await blog_service.get_article_content(article_id)


@tool
async def search_external_tech_blogs(query: str) -> List[dict]:
    """
    搜索外部技术博客

    使用 Tavily 搜索引擎搜索外部高质量技术博客文章。

    Args:
        query: 搜索查询语句

    Returns:
        外部技术文章列表
    """
    tavily = _create_tavily_tool()
    if tavily is None:
        return []

    try:
        logger.info(f"[ExternalSearch] 开始搜索: {query}")
        results = await tavily.ainvoke({"query": query})

        if isinstance(results, dict) and "results" in results:
            return results["results"]
        if isinstance(results, list):
            return results

        logger.warning(f"[ExternalSearch] 未知的返回格式: {type(results)}")
        return []

    except Exception as e:
        logger.error(f"[ExternalSearch] 搜索失败: {e}", exc_info=True)
        return []
