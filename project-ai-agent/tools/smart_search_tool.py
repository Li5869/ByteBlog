"""
智能搜索工具管理器
文件：project-ai-agent/tools/smart_search_tool.py
"""

from typing import List, Dict, Any
from loguru import logger
from langchain_core.tools import tool
from config.settings import get_settings


class SmartSearchManager:
    """智能搜索管理器 - 协调站内和外部搜索"""
    
    def __init__(self):
        self.min_internal_results = 3
        self.max_total_results = 10
        
        if not get_settings().tavily_api_key:
            logger.warning("[SmartSearch] TAVILY_API_KEY 未配置，外部搜索功能将不可用")
    
    async def smart_search(
        self,
        keywords: List[str],
        topic: str = "",
        enable_external: bool = True
    ) -> List[Dict[str, Any]]:
        """
        智能搜索策略
        
        Args:
            keywords: 搜索关键词列表
            topic: 文章主题（用于外部搜索）
            enable_external: 是否启用外部搜索
        
        Returns:
            合并后的搜索结果列表
        """
        from tools.article_tool import search_articles_by_keyword, search_external_tech_blogs
        
        all_results = []
        internal_count = 0
        external_count = 0
        
        for keyword in keywords[:3]:
            try:
                logger.info(f"[SmartSearch] 站内搜索: {keyword}")
                internal_results = await search_articles_by_keyword.ainvoke({"keyword": keyword})
                
                if isinstance(internal_results, list) and len(internal_results) > 0:
                    all_results.extend(self._format_internal_results(internal_results[:2]))
                    internal_count += len(internal_results)
                    logger.info(f"[SmartSearch] 站内找到 {len(internal_results)} 条")
                
                if enable_external and len(all_results) < self.min_internal_results:
                    logger.info(f"[SmartSearch] 站内结果不足({len(all_results)}<{self.min_internal_results})，启动外部搜索: {keyword}")
                    
                    try:
                        external_results = await search_external_tech_blogs.ainvoke({"query": keyword})
                        
                        if isinstance(external_results, list) and len(external_results) > 0:
                            all_results.extend(self._format_external_results(external_results[:2]))
                            external_count += len(external_results)
                            logger.info(f"[SmartSearch] 外部找到 {len(external_results)} 条")
                        else:
                            logger.warning(f"[SmartSearch] 外部搜索无结果")
                    except Exception as ext_e:
                        logger.error(f"[SmartSearch] 外部搜索异常: {ext_e}")
                
            except Exception as e:
                logger.warning(f"[SmartSearch] 搜索失败 '{keyword}': {e}")
        
        if enable_external and len(all_results) < self.min_internal_results and topic:
            logger.info(f"[SmartSearch] 结果不足({len(all_results)}<{self.min_internal_results})，使用主题补充搜索: {topic}")
            try:
                external_results = await search_external_tech_blogs.ainvoke({"query": topic})
                if isinstance(external_results, list):
                    formatted = self._format_external_results(external_results[:3])
                    all_results.extend(formatted)
                    external_count += len(external_results)
                    logger.info(f"[SmartSearch] 主题搜索找到 {len(external_results)} 条")
            except Exception as e:
                logger.warning(f"[SmartSearch] 主题搜索失败: {e}")
        
        final_results = all_results[:self.max_total_results]
        
        logger.info(
            f"[SmartSearch] 搜索完成 - "
            f"站内: {internal_count}条, 外部: {external_count}条, "
            f"总计: {len(final_results)}条"
        )
        
        return final_results
    
    def _format_internal_results(self, results: List[Dict]) -> List[Dict]:
        """格式化站内搜索结果"""
        formatted = []
        for item in results:
            formatted.append({
                "source": "internal",
                "title": item.get("title", ""),
                "summary": item.get("summary", ""),
                "author": item.get("author_name", ""),
                "views": item.get("views", 0),
                "url": f"/article/{item.get('id')}",
                "score": item.get("score", 0)
            })
        return formatted
    
    def _format_external_results(self, results: List[Dict]) -> List[Dict]:
        """格式化外部搜索结果"""
        formatted = []
        for item in results:
            formatted.append({
                "source": "external",
                "title": item.get("title", ""),
                "summary": item.get("content", "")[:200] if item.get("content") else item.get("summary", ""),
                "author": item.get("author", ""),
                "url": item.get("url", ""),
                "score": item.get("score", 0)
            })
        return formatted


_smart_search_manager: SmartSearchManager = None


def get_smart_search_manager() -> SmartSearchManager:
    """获取智能搜索管理器单例"""
    global _smart_search_manager
    if _smart_search_manager is None:
        _smart_search_manager = SmartSearchManager()
    return _smart_search_manager


@tool
async def smart_search_references(
    keywords: List[str],
    topic: str = ""
) -> List[Dict[str, Any]]:
    """
    智能搜索参考资料
    
    自动协调站内和外部搜索，优先返回站内结果，
    如果站内结果不足则补充外部高质量技术博客。
    
    Args:
        keywords: 搜索关键词列表
        topic: 文章主题（可选，用于补充搜索）
    
    Returns:
        合并后的参考资料列表
    """
    manager = get_smart_search_manager()
    return await manager.smart_search(keywords, topic, enable_external=True)
