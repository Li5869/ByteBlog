"""
Skill 工具
文件：project-ai-agent/tools/skill_tool.py
功能：提供 Skill 相关的工具，支持渐进式披露
"""

from langchain_core.tools import tool
from loguru import logger

from skills.loader import get_skill_loader


@tool
def get_skill_details(skill_name: str) -> str:
    """
    获取指定 Skill 的详细信息。
    
    当你需要了解某个 Skill 的完整使用说明、触发条件、工具列表时调用此工具。
    返回该 Skill 的完整 Markdown 内容，包含概述、触发条件、工具列表、工作流程、示例等。
    
    Args:
        skill_name: Skill 名称，可选值包括：
            - article-search: 文章搜索与推荐
            - author-discovery: 博主探索
            - knowledge-qa: 知识库问答
            - smart-chat: 智能对话
            - smart-search: 综合搜索
    
    Returns:
        该 Skill 的完整 Markdown 内容
    """
    loader = get_skill_loader()
    content = loader.get_skill_content(skill_name)
    
    if content:
        logger.info(f"[SkillTool] 获取 Skill 详情: {skill_name}")
        return content
    
    available_skills = loader.list_skills()
    return f"Skill '{skill_name}' 不存在。\n\n**可用的 Skills**：\n" + "\n".join(f"- {name}" for name in available_skills)


@tool
def list_available_skills() -> str:
    """
    列出所有可用的 Skills 及其简要描述。
    
    当你不确定有哪些 Skills 可用，或需要快速了解每个 Skill 的用途时调用此工具。
    返回所有 Skills 的名称和简要描述列表。
    
    Returns:
        所有 Skills 的名称和简要描述列表
    """
    loader = get_skill_loader()
    skills = loader.load()
    
    if not skills:
        return "暂无可用的 Skills"
    
    lines = ["**可用的 Skills**：", ""]
    for i, (name, skill) in enumerate(skills.items(), 1):
        lines.append(f"{i}. **{name}**：{skill['description']}")
    
    return "\n".join(lines)
