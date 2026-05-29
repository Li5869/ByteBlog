"""
网页爬取工具
文件：project-ai-agent/tools/web_scraper_tool.py

功能：提供网页爬取的 LangChain Tool 定义，具体逻辑由 WebScraperService 实现。
"""

import asyncio
from loguru import logger
from langchain_core.tools import tool

from services.business.web_scraper_service import get_web_scraper_service


@tool
async def scrape_webpage(
    url: str,
    output_format: str = "markdown",
    include_metadata: bool = True,
) -> dict:
    """
    爬取网页内容

    获取指定 URL 的网页内容，提取正文并转换为 Markdown 格式。
    适用于爬取技术博客、文档、文章等网页内容。

    Args:
        url: 要爬取的网页 URL（必须是完整的 http/https 链接）
        output_format: 输出格式，可选 "markdown" 或 "text"，默认 "markdown"
        include_metadata: 是否包含元数据（标题、作者、发布时间），默认 True

    Returns:
        包含以下字段的字典：
        - success: 是否成功
        - content: 正文内容（Markdown 或纯文本）
        - title: 文章标题（如果 include_metadata=True）
        - author: 作者（如果 include_metadata=True）
        - date: 发布日期（如果 include_metadata=True）
        - url: 原始 URL
        - word_count: 正文字数
        - error: 错误信息（如果失败）

    使用场景：
    1. 用户分享了一个链接，需要查看内容
    2. 搜索到外部文章，需要获取完整内容作为参考资料
    3. AI 写作时需要引用外部资料的内容
    4. 知识库导入时需要爬取网页内容
    """
    service = get_web_scraper_service()
    return await service.scrape_webpage(url, output_format, include_metadata)


@tool
async def scrape_multiple_webpages(
    urls: list[str],
    output_format: str = "markdown",
) -> list[dict]:
    """
    批量爬取多个网页内容

    并发爬取多个 URL 的网页内容，适用于需要同时获取多篇参考资料的场景。

    Args:
        urls: 要爬取的网页 URL 列表（最多 5 个）
        output_format: 输出格式，可选 "markdown" 或 "text"，默认 "markdown"

    Returns:
        爬取结果列表，每个元素包含 success、content、title 等字段
    """
    # 限制批量数量
    urls = urls[:5]

    logger.info(f"[WebScraper] 批量爬取 {len(urls)} 个网页")

    service = get_web_scraper_service()

    # 并发执行
    tasks = [service.scrape_webpage(url, output_format) for url in urls]
    results = await asyncio.gather(*tasks, return_exceptions=True)

    # 处理异常
    final_results = []
    for i, result in enumerate(results):
        if isinstance(result, Exception):
            final_results.append({
                "success": False,
                "url": urls[i],
                "error": f"爬取异常: {str(result)}",
            })
        else:
            final_results.append(result)

    success_count = sum(1 for r in final_results if r.get("success"))
    logger.info(f"[WebScraper] 批量爬取完成: {success_count}/{len(urls)} 成功")

    return final_results


# 工具列表
WEB_SCRAPER_TOOLS = [scrape_webpage, scrape_multiple_webpages]
