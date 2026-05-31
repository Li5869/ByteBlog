"""
工具注册表

"""

from config.settings import get_settings
from tools.article_tool import (
    search_articles_by_keyword,
    get_hot_articles,
    get_article_by_id,
    get_article_content_by_id,
    search_external_tech_blogs,
)
from tools.common_tool import get_the_time
from tools.vector_tool import (
    search_knowledge_base,
)
from tools.author_tool import (
    search_authors_by_keyword,
    get_hot_authors,
    get_author_by_id,
)
from tools.blog_tool import (
    get_category_list,
)
from tools.user_tool import get_current_user_id, get_current_user_info, set_current_user_id
from tools.skill_tool import get_skill_details, list_available_skills, search_skill_guide
from tools.writing_tool import (
    writing_start,
    writing_status,
    writing_action,
    writing_result,
    writing_publish,
)
from tools.web_scraper_tool import (
    scrape_webpage,
    scrape_multiple_webpages,
)

# ==================== 工具分组 ====================

_base_tools = [
    search_articles_by_keyword,
    get_hot_articles,
    get_article_by_id,
    get_article_content_by_id,
    search_knowledge_base,
    search_authors_by_keyword,
    get_hot_authors,
    get_author_by_id,
    get_category_list,
    get_the_time,
    get_current_user_id,
    get_current_user_info,
    # Skill 工具（渐进式披露）
    get_skill_details,
    list_available_skills,
    search_skill_guide,
    # 写作助手工具（5个）
    writing_start,
    writing_status,
    writing_action,
    writing_result,
    writing_publish,
    # 网页爬取工具（2个）
    scrape_webpage,
    scrape_multiple_webpages,
]

_tavily_tools = [search_external_tech_blogs]


def _get_tools_with_tavily():
    """根据配置动态加载工具（Tavily 需要 API Key）"""
    tools = _base_tools.copy()
    if get_settings().tavily_api_key:
        tools.extend(_tavily_tools)
    return tools


ALL_TOOLS = _get_tools_with_tavily()


def get_all_tools():
    """获取所有工具"""
    return ALL_TOOLS


def get_search_tools():
    """获取搜索相关工具"""
    return [search_articles_by_keyword, search_knowledge_base, search_authors_by_keyword]


def get_article_tools():
    """获取文章相关工具"""
    return [get_hot_articles, get_article_by_id, get_article_content_by_id]


def get_author_tools():
    """获取博主相关工具"""
    return [search_authors_by_keyword, get_hot_authors, get_author_by_id]
