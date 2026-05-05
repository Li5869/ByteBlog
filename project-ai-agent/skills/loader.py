"""
Skills 加载器
文件：project-ai-agent/skills/loader.py
功能：从 skills 目录加载所有 SKILL.md 文件，解析为可用的提示词片段
"""

import os
from pathlib import Path
from typing import Optional
import yaml
from loguru import logger


class SkillLoader:
    """
    Skills 加载器
    
    从 skills 目录加载所有 SKILL.md 文件，解析 frontmatter 和内容，
    提供给 SmartAgent 使用。
    
    使用方式：
        loader = get_skill_loader()
        descriptions = loader.get_skill_descriptions()
        content = loader.get_skill_content("article-search")
    """

    def __init__(self, skills_dir: str | Path = "skills"):
        """
        初始化 Skills 加载器
        
        Args:
            skills_dir: Skills 目录路径，默认为 "skills"
        """
        self.skills_dir = Path(skills_dir)
        self._skills: dict = {}
        self._loaded = False

    def load(self) -> dict:
        """
        加载所有 Skills
        
        Returns:
            dict: {skill_name: {name, description, content, path}}
        """
        if self._loaded:
            return self._skills

        if not self.skills_dir.exists():
            logger.warning(f"[SkillLoader] Skills 目录不存在: {self.skills_dir}")
            return self._skills

        for skill_dir in self.skills_dir.iterdir():
            if not skill_dir.is_dir():
                continue

            skill_file = skill_dir / "SKILL.md"
            if not skill_file.exists():
                continue

            try:
                content = skill_file.read_text(encoding="utf-8")
                skill_data = self._parse_skill(content)
                if skill_data:
                    self._skills[skill_data["name"]] = {
                        "name": skill_data["name"],
                        "description": skill_data["description"],
                        "content": content,
                        "path": str(skill_file),
                    }
            except Exception as e:
                logger.error(f"[SkillLoader] 加载失败: {skill_dir}, error={e}")

        self._loaded = True
        logger.info(f"[SkillLoader] 加载完成: {len(self._skills)} 个 Skills")
        return self._skills

    def _parse_skill(self, content: str) -> Optional[dict]:
        """
        解析 SKILL.md 文件
        
        格式：
        ---
        name: "skill-name"
        description: "skill description"
        ---
        # Skill content...
        
        Args:
            content: SKILL.md 文件内容
            
        Returns:
            解析后的 dict，包含 name 和 description
        """
        if not content.startswith("---"):
            return None

        parts = content.split("---", 2)
        if len(parts) < 3:
            return None

        try:
            frontmatter = yaml.safe_load(parts[1])
            if not frontmatter:
                return None

            name = frontmatter.get("name", "")
            description = frontmatter.get("description", "")

            if not name:
                return None

            return {
                "name": name,
                "description": description,
            }
        except yaml.YAMLError:
            return None

    def get_skill_descriptions(self) -> str:
        """
        获取所有 Skills 的描述列表（完整版）
        
        用于替代原有的 _build_strategy_guide()，
        将 Skills 的 description 字段格式化输出。
        
        Returns:
            格式化的 Skills 描述字符串
        """
        skills = self.load()
        
        if not skills:
            return "**可用技能**：暂无"

        lines = ["**可用技能（Skills）**：", ""]
        lines.append("以下是你可以使用的技能，每个技能对应一类任务场景：")
        lines.append("")

        for name, skill in skills.items():
            lines.append(f"**{name}**：")
            lines.append(f"  {skill['description']}")
            lines.append("")

        return "\n".join(lines)

    def get_skill_brief(self) -> str:
        """
        获取所有 Skills 的简要描述（用于渐进式披露）
        
        只包含 Skill 名称和描述，不包含详细内容。
        用于系统提示词，当 Agent 需要详细信息时调用 get_skill_details 工具。
        
        Returns:
            简要的 Skills 描述字符串
        """
        skills = self.load()
        
        if not skills:
            return "**可用技能**：暂无"

        lines = ["**可用技能（Skills）**：", ""]
        lines.append("以下是可用的技能列表。如需了解某个技能的详细用法，请调用 `get_skill_details` 工具。")
        lines.append("")

        for name, skill in skills.items():
            brief_desc = skill['description'][:80] + "..." if len(skill['description']) > 80 else skill['description']
            lines.append(f"- **{name}**：{brief_desc}")

        return "\n".join(lines)

    def get_skill_content(self, skill_name: str) -> Optional[str]:
        """
        获取指定 Skill 的完整内容
        
        Args:
            skill_name: Skill 名称
            
        Returns:
            Skill 的完整 Markdown 内容
        """
        skills = self.load()
        if skill_name in skills:
            return skills[skill_name]["content"]
        return None

    def get_all_skill_contents(self) -> str:
        """
        获取所有 Skills 的完整内容（合并）
        
        当需要将所有 Skills 注入到系统提示词时使用。
        注意：内容较长，可能占用较多 token。
        
        Returns:
            所有 Skills 合并后的 Markdown 内容
        """
        skills = self.load()
        
        if not skills:
            return ""

        parts = []
        for name, skill in skills.items():
            parts.append(f"---\n# Skill: {name}\n\n{skill['content']}\n")

        return "\n".join(parts)

    def list_skills(self) -> list[str]:
        """
        列出所有 Skill 名称
        
        Returns:
            Skill 名称列表
        """
        skills = self.load()
        return list(skills.keys())

    def debug_print(self) -> None:
        """
        打印所有 Skills 的详细信息（用于调试）
        
        在控制台输出每个 Skill 的完整信息，方便调试和验证。
        """
        skills = self.load()
        
        print("\n" + "=" * 60)
        print("📋 Skills 加载状态")
        print("=" * 60)
        print(f"目录: {self.skills_dir.absolute()}")
        print(f"已加载: {len(skills)} 个 Skills")
        print("-" * 60)
        
        if not skills:
            print("⚠️  无 Skills 被加载")
        else:
            for i, (name, skill) in enumerate(skills.items(), 1):
                print(f"\n{i}. **{name}**")
                print(f"   路径: {skill['path']}")
                print(f"   描述: {skill['description'][:80]}...")
                print(f"   内容长度: {len(skill['content'])} 字符")
        
        print("\n" + "=" * 60 + "\n")

    def reload(self) -> dict:
        """
        重新加载所有 Skills
        
        用于热更新场景，当 Skills 文件被修改后调用。
        
        Returns:
            重新加载后的 Skills dict
        """
        self._loaded = False
        self._skills = {}
        return self.load()


# ==================== 单例 ====================

_skill_loader: Optional[SkillLoader] = None


def get_skill_loader(skills_dir: str | Path = "skills") -> SkillLoader:
    """
    获取 Skills 加载器单例
    
    Args:
        skills_dir: Skills 目录路径
        
    Returns:
        SkillLoader 实例
    """
    global _skill_loader
    if _skill_loader is None:
        _skill_loader = SkillLoader(skills_dir)
    return _skill_loader


def reload_skills() -> dict:
    """
    重新加载所有 Skills（便捷函数）
    
    Returns:
        重新加载后的 Skills dict
    """
    return get_skill_loader().reload()
