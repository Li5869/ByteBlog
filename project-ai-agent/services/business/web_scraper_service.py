"""
网页爬取服务
文件：project-ai-agent/services/business/web_scraper_service.py

功能：提供网页爬取和内容提取的核心业务逻辑。
"""

from typing import Optional
from functools import lru_cache
from loguru import logger


@lru_cache
def get_web_scraper_service() -> "WebScraperService":
    """获取网页爬取服务单例（@lru_cache 保证单例）"""
    return WebScraperService()


class WebScraperService:
    """网页爬取服务 - 提供网页内容爬取和提取功能"""

    def __init__(self):
        self._trafilatura = None
        self._httpx = None

    def _get_trafilatura(self):
        """延迟导入 trafilatura"""
        if self._trafilatura is None:
            try:
                import trafilatura
                self._trafilatura = trafilatura
            except ImportError:
                logger.error("trafilatura 未安装，请运行: pip install trafilatura")
                return None
        return self._trafilatura

    def _get_httpx(self):
        """延迟导入 httpx"""
        if self._httpx is None:
            try:
                import httpx
                self._httpx = httpx
            except ImportError:
                logger.error("httpx 未安装，请运行: pip install httpx")
                return None
        return self._httpx

    async def fetch_html(self, url: str, timeout: float = 30.0) -> Optional[str]:
        """
        获取网页 HTML 内容

        Args:
            url: 网页 URL
            timeout: 请求超时时间（秒）

        Returns:
            HTML 内容字符串，失败返回 None
        """
        httpx = self._get_httpx()
        if httpx is None:
            return None

        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                          "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        }

        try:
            async with httpx.AsyncClient(
                follow_redirects=True,
                timeout=httpx.Timeout(connect=5.0, read=timeout, write=5.0, pool=5.0),
            ) as client:
                response = await client.get(url, headers=headers)
                response.raise_for_status()
                return response.text
        except httpx.TimeoutException:
            logger.warning(f"[WebScraper] 请求超时: {url}")
            return None
        except httpx.HTTPStatusError as e:
            logger.warning(f"[WebScraper] HTTP 错误 {e.response.status_code}: {url}")
            return None
        except Exception as e:
            logger.error(f"[WebScraper] 请求失败: {url}, 错误: {e}")
            return None

    def extract_content(self, html: str, url: str, output_format: str = "markdown") -> Optional[str]:
        """
        从 HTML 中提取正文内容

        Args:
            html: HTML 内容
            url: 网页 URL（用于提取元数据）
            output_format: 输出格式，支持 "markdown" 或 "text"

        Returns:
            提取的正文内容，失败返回 None
        """
        trafilatura = self._get_trafilatura()
        if trafilatura is None:
            return None

        try:
            # 使用 trafilatura 提取正文
            # favor_recall=True 提高召回率，适合技术文档
            result = trafilatura.extract(
                html,
                url=url,
                output_format=output_format,
                include_comments=False,
                include_tables=True,
                include_links=True,
                include_images=False,
                favor_recall=True,
                deduplicate=True,
            )
            return result
        except Exception as e:
            logger.error(f"[WebScraper] 内容提取失败: {url}, 错误: {e}")
            return None

    def extract_metadata(self, html: str, url: str) -> dict:
        """
        提取网页元数据（标题、作者、发布时间等）

        Args:
            html: HTML 内容
            url: 网页 URL

        Returns:
            元数据字典
        """
        trafilatura = self._get_trafilatura()
        if trafilatura is None:
            return {}

        try:
            import json

            metadata = trafilatura.extract(
                html,
                url=url,
                output_format="json",
                include_comments=False,
            )

            if metadata and isinstance(metadata, str):
                data = json.loads(metadata)
                return {
                    "title": data.get("title", ""),
                    "author": data.get("author", ""),
                    "date": data.get("date", ""),
                    "sitename": data.get("sitename", ""),
                    "categories": data.get("categories", []),
                    "tags": data.get("tags", []),
                }
            return {}
        except Exception as e:
            logger.warning(f"[WebScraper] 元数据提取失败: {url}, 错误: {e}")
            return {}

    async def scrape_webpage(
        self,
        url: str,
        output_format: str = "markdown",
        include_metadata: bool = True,
    ) -> dict:
        """
        爬取网页内容（完整流程）

        Args:
            url: 要爬取的网页 URL
            output_format: 输出格式，支持 "markdown" 或 "text"
            include_metadata: 是否包含元数据

        Returns:
            包含爬取结果的字典
        """
        logger.info(f"[WebScraper] 开始爬取: {url}")

        # 参数校验
        if not url.startswith(("http://", "https://")):
            return {
                "success": False,
                "url": url,
                "error": "URL 必须以 http:// 或 https:// 开头",
            }

        if output_format not in ("markdown", "text"):
            return {
                "success": False,
                "url": url,
                "error": "output_format 必须是 'markdown' 或 'text'",
            }

        # 获取 HTML
        html = await self.fetch_html(url)
        if html is None:
            return {
                "success": False,
                "url": url,
                "error": "无法获取网页内容，请检查 URL 是否正确或网络连接",
            }

        # 提取正文
        content = self.extract_content(html, url, output_format)
        if content is None:
            return {
                "success": False,
                "url": url,
                "error": "无法提取网页正文内容，可能是动态加载页面或反爬机制",
            }

        # 构建结果
        result = {
            "success": True,
            "url": url,
            "content": content,
            "word_count": len(content),
            "format": output_format,
        }

        # 提取元数据
        if include_metadata:
            metadata = self.extract_metadata(html, url)
            result.update({
                "title": metadata.get("title", ""),
                "author": metadata.get("author", ""),
                "date": metadata.get("date", ""),
                "sitename": metadata.get("sitename", ""),
            })

        logger.info(
            f"[WebScraper] 爬取成功: {url}, "
            f"字数: {result['word_count']}, "
            f"标题: {result.get('title', '未知')}"
        )

        return result
