"""
智能搜索工具
文件：project-ai-agent/tools/smart_search_tool.py

设计思路：不预设站内/站外搜索策略，始终并行搜索两端，站内结果优先排列。
下游 LLM 在生成内容时会根据上下文自行决定参考哪些资料，比硬编码阈值更灵活。
"""

from typing import List, Dict, Any
from loguru import logger
from langchain_core.tools import tool


MAX_TOTAL_RESULTS = 10


async def _do_search(keyword: str) -> List[Dict[str, Any]]:
    """对单个关键词并行搜索站内+站外，站内优先"""
    from tools.article_tool import search_articles_by_keyword, search_external_tech_blogs

    results = []

    # 站内搜索
    try:
        internal = await search_articles_by_keyword.ainvoke({"keyword": keyword})
        if isinstance(internal, list) and internal:
            for item in internal[:2]:
                results.append({
                    "source": "internal",
                    "title": item.title,
                    "summary": item.summary,
                    "author": item.author_name,
                    "views": item.views,
                    "url": f"/article/{item.id}",
                    "score": item.score
                })
            logger.info(f"[SmartSearch] 站内 '{keyword}': {len(internal)} 条")
    except Exception as e:
        logger.warning(f"[SmartSearch] 站内搜索失败 '{keyword}': {e}")

    # 外部搜索
    try:
        external = await search_external_tech_blogs.ainvoke({"query": keyword})
        if isinstance(external, list) and external:
            for item in external[:2]:
                results.append({
                    "source": "external",
                    "title": item.get("title", ""),
                    "summary": (item.get("content", "") or "")[:200],
                    "author": item.get("author", ""),
                    "url": item.get("url", ""),
                    "score": item.get("score", 0)
                })
            logger.info(f"[SmartSearch] 外部 '{keyword}': {len(external)} 条")
    except Exception as e:
        logger.warning(f"[SmartSearch] 外部搜索失败 '{keyword}': {e}")

    return results


@tool
async def smart_search_references(
    keywords: List[str],
    topic: str = ""
) -> List[Dict[str, Any]]:
    """
    智能搜索参考资料

    同时搜索站内文章和外部技术博客，站内结果优先排列。
    由下游 LLM 根据上下文自行判断参考哪些资料，不做硬编码的补搜决策。

    Args:
        keywords: 搜索关键词列表（最多取前3个）
        topic: 文章主题（备用，当关键词搜不到结果时使用）

    Returns:
        合并后的参考资料列表（最多10条，站内优先）
    """
    all_results = []

    for keyword in keywords[:3]:
        try:
            all_results.extend(await _do_search(keyword))
        except Exception as e:
            logger.warning(f"[SmartSearch] 关键词 '{keyword}' 搜索异常: {e}")

    # 如果关键词都没搜到任何结果，用主题兜底
    if not all_results and topic:
        logger.info(f"[SmartSearch] 关键词无结果，使用主题兜底: {topic}")
        try:
            all_results.extend(await _do_search(topic))
        except Exception as e:
            logger.warning(f"[SmartSearch] 主题兜底搜索失败: {e}")

    final_results = all_results[:MAX_TOTAL_RESULTS]
    internal_count = sum(1 for r in final_results if r.get("source") == "internal")
    external_count = sum(1 for r in final_results if r.get("source") == "external")

    logger.info(
        f"[SmartSearch] 搜索完成 - 站内: {internal_count}条, 外部: {external_count}条, 总计: {len(final_results)}条"
    )

    return final_results
