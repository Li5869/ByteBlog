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
def get_blog_tools() -> List:
    """
    获取博客工具列表

    Returns:
        博客工具列表
    """
    return [get_category_list]
