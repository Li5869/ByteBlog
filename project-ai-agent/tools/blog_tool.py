"""
博客工具集
文件：project-ai-agent/tools/blog_tool.py
"""

from langchain_core.tools import tool
from services.business.blog_service import get_blog_service
from typing import List


@tool
async def get_category_list() -> List[dict] | str:
    """
    获取博客分类列表

    获取博客系统中所有的文章分类信息。
    适用于用户想要了解博客有哪些分类或按分类浏览文章。

    Returns:
        分类列表，每个分类包含 id、name 等字段；
        失败时返回友好错误提示字符串
    """
    try:
        blog_service = get_blog_service()
        categories = await blog_service.get_categories()
        return categories
    except Exception as e:
        return f"获取分类列表失败：{str(e)}"


@tool
async def get_hot_tag_list(limit: int = 20) -> List[dict] | str:
    """
    获取热门标签列表

    获取博客系统中使用频率最高的标签。
    适用于用户想要了解热门话题或按标签浏览文章。

    Args:
        limit: 返回数量限制，默认20条

    Returns:
        热门标签列表，每个标签包含 id、name、count 等字段；
        失败时返回友好错误提示字符串
    """
    try:
        blog_service = get_blog_service()
        tags = await blog_service.get_hot_tags(limit)
        return tags
    except Exception as e:
        return f"获取热门标签失败：{str(e)}"


def get_blog_tools() -> List:
    """
    获取博客工具列表

    Returns:
        博客工具列表，包含 get_category_list 和 get_hot_tag_list
    """
    return [get_category_list, get_hot_tag_list]
