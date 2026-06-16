"""
Firecrawl 网页爬取工具
文件：project-ai-agent/tools/firecrawl_tool.py

提供 Firecrawl 的 LangChain Tool 定义，具体逻辑由 FirecrawlService 实现。
支持 JS 渲染页面和反爬虫处理，作为 scrape_webpage 的增强方案。
"""

from langchain_core.tools import tool

from mcp_service.firecrawl_service import get_firecrawl_service


@tool
async def firecrawl_scrape(
    url: str,
) -> dict:
    """
    使用 Firecrawl 爬取网页内容（支持 JS 渲染和反爬虫）

    当用户需要查看某个链接的完整内容时使用此工具。
    适用于 JS 渲染的 SPA 页面、有反爬虫保护的网站。
    普通网页优先使用 scrape_webpage，此工具作为增强方案。

    Args:
        url: 要爬取的网页 URL（必须是完整的 http/https 链接）

    Returns:
        包含以下字段的字典：
        - success: 是否成功
        - markdown: 正文内容（Markdown 格式）
        - metadata: 页面元数据（标题、描述等）
        - url: 原始 URL
        - word_count: 正文字数
        - error: 错误信息（如果失败）

    使用场景：
    1. 用户分享了一个需要 JS 渲染的链接
    2. 使用 scrape_webpage 失败时的备选方案
    3. 需要高质量 Markdown 提取的场景
    """
    service = get_firecrawl_service()
    return await service.scrape_url(url)


@tool
async def firecrawl_search(
    query: str,
    limit: int = 5,
) -> dict:
    """
    使用 Firecrawl 搜索网页并返回内容

    当需要搜索外部资源并获取完整页面内容时使用此工具。
    相比普通搜索引擎，Firecrawl 会返回干净的 Markdown 内容。

    Args:
        query: 搜索查询关键词
        limit: 返回结果数量，默认 5，最大 10

    Returns:
        包含以下字段的字典：
        - success: 是否成功
        - query: 搜索查询
        - results: 搜索结果列表，每项包含 title、url、content
        - total: 结果总数
        - error: 错误信息（如果失败）

    使用场景：
    1. 用户要求搜索某个技术主题的详细资料
    2. 需要获取搜索结果的完整内容（而非摘要）
    3. Tavily 搜索结果不足时的补充方案
    """
    # 限制结果数量
    limit = min(limit, 10)

    service = get_firecrawl_service()
    return await service.search(query, limit=limit)


# 工具列表
FIRECRAWL_TOOLS = [firecrawl_scrape, firecrawl_search]
