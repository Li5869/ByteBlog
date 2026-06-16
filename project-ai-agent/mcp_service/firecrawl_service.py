"""
Firecrawl 网页爬取服务
文件：project-ai-agent/mcp_service/firecrawl_service.py

封装 Firecrawl 云服务 API，提供网页爬取、搜索等功能。
支持 JS 渲染页面和反爬虫处理，作为自研爬虫的增强方案。
"""

from typing import Optional
from functools import lru_cache

from loguru import logger


@lru_cache
def get_firecrawl_service() -> "FirecrawlService":
    """获取 Firecrawl 服务单例（@lru_cache 保证单例）"""
    return FirecrawlService()


class FirecrawlService:
    """
    Firecrawl 网页爬取服务

    封装 firecrawl-py SDK，提供统一的爬取接口。
    当 API Key 未配置或调用失败时返回错误信息，由调用方决定降级策略。
    """

    def __init__(self):
        self._app = None
        self._api_key: Optional[str] = None

    def _get_app(self):
        """延迟初始化 FirecrawlApp 实例"""
        if self._app is None:
            try:
                from config.settings import get_settings
                settings = get_settings()
                self._api_key = settings.firecrawl_api_key

                if not self._api_key:
                    logger.warning("[Firecrawl] API Key 未配置，服务不可用")
                    return None

                from firecrawl import Firecrawl
                self._app = Firecrawl(api_key=self._api_key)
                logger.info("[Firecrawl] 服务初始化成功")
            except ImportError:
                logger.error("[Firecrawl] firecrawl-py 未安装，请运行: pip install firecrawl-py")
                return None
            except Exception as e:
                logger.error(f"[Firecrawl] 初始化失败: {e}")
                return None
        return self._app

    async def scrape_url(
        self,
        url: str,
        formats: Optional[list] = None,
    ) -> dict:
        """
        爬取单个网页内容

        Args:
            url: 要爬取的网页 URL
            formats: 输出格式列表，默认 ["markdown"]

        Returns:
            包含以下字段的字典：
            - success: 是否成功
            - url: 原始 URL
            - markdown: Markdown 格式的正文内容
            - metadata: 页面元数据（标题、描述等）
            - error: 错误信息（失败时）
        """
        app = self._get_app()
        if app is None:
            return {
                "success": False,
                "url": url,
                "error": "Firecrawl 服务未配置（缺少 API Key）",
            }

        if formats is None:
            formats = ["markdown"]

        try:
            logger.info(f"[Firecrawl] 开始爬取: {url}")
            doc = app.scrape(url, formats=formats)

            # SDK 返回 Pydantic Document 对象，使用属性访问
            markdown_content = doc.markdown or ""
            metadata = doc.metadata

            # 提取元数据（metadata 也是 Pydantic 对象）
            meta_title = metadata.title if metadata else ""
            meta_desc = metadata.description if metadata else ""
            meta_lang = metadata.language if metadata else ""

            logger.info(
                f"[Firecrawl] 爬取成功: {url}, "
                f"字数: {len(markdown_content)}, "
                f"标题: {meta_title or '未知'}"
            )

            return {
                "success": True,
                "url": url,
                "markdown": markdown_content,
                "metadata": {
                    "title": meta_title or "",
                    "description": meta_desc or "",
                    "language": meta_lang or "",
                },
                "word_count": len(markdown_content),
            }

        except Exception as e:
            logger.error(f"[Firecrawl] 爬取失败: {url}, 错误: {e}")
            return {
                "success": False,
                "url": url,
                "error": f"Firecrawl 爬取失败: {str(e)}",
            }

    async def search(
        self,
        query: str,
        limit: int = 5,
    ) -> dict:
        """
        使用 Firecrawl 搜索网页并返回内容

        Args:
            query: 搜索查询
            limit: 返回结果数量，默认 5

        Returns:
            包含以下字段的字典：
            - success: 是否成功
            - query: 搜索查询
            - results: 搜索结果列表，每项包含 title、url、content
            - error: 错误信息（失败时）
        """
        app = self._get_app()
        if app is None:
            return {
                "success": False,
                "query": query,
                "error": "Firecrawl 服务未配置（缺少 API Key）",
            }

        try:
            logger.info(f"[Firecrawl] 搜索: {query}")
            search_data = app.search(query, limit=limit)

            # SDK 返回 Pydantic SearchData 对象，结果在 .web 属性中
            web_results = search_data.web or []
            results = []
            for item in web_results:
                # item 是 SearchResultWeb 或 Document 对象，使用属性访问
                results.append({
                    "title": getattr(item, "title", "") or "",
                    "url": getattr(item, "url", "") or "",
                    "content": getattr(item, "markdown", "") or "",
                })

            logger.info(f"[Firecrawl] 搜索完成: {query}, 结果数: {len(results)}")

            return {
                "success": True,
                "query": query,
                "results": results,
                "total": len(results),
            }

        except Exception as e:
            logger.error(f"[Firecrawl] 搜索失败: {query}, 错误: {e}")
            return {
                "success": False,
                "query": query,
                "error": f"Firecrawl 搜索失败: {str(e)}",
            }
