"""
Skills 模块
文件：project-ai-agent/skills/__init__.py
"""

from skills.loader import SkillLoader, get_skill_loader, reload_skills


def debug_skills() -> None:
    """
    打印 Skills 加载状态（便捷函数）
    
    用于调试和验证 Skills 是否正确加载。
    """
    loader = get_skill_loader()
    loader.debug_print()


__all__ = [
    "SkillLoader",
    "get_skill_loader",
    "reload_skills",
    "debug_skills",
]
