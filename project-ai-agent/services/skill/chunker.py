"""
Skill 文档语义切片器
文件：project-ai-agent/services/skill/chunker.py

将 SKILL.md 按 Markdown 标题边界切分为独立 chunk，每个 chunk 是一个完整语义单元。
"""

import re
from pathlib import Path
from typing import Optional

import yaml
from loguru import logger

from models.schemas import SkillChunk


_HEADING_RE = re.compile(r"^(#{2,3})\s+(.+)$", re.MULTILINE)


class SkillChunker:
    """
    语义切片器：将 SKILL.md 按 Markdown 标题边界切分为独立 chunk

    切片逻辑：
    1. 按正则 ^#{2,3}\\s+(.+) 找到所有标题位置作为分割边界
    2. 遍历标题段生成 SkillChunk:
       - ## 级别的段直接成为一个 chunk
       - ### 级别也独立成 chunk（工具详细说明等细粒度检索）
       - 标题之间的内容合并到所属 chunk
    3. 为每个 chunk 注入 skill_name + source_path
    """

    def __init__(self, skills_dir: str | Path = "skills"):
        self.skills_dir = Path(skills_dir)

    def chunk_skill(
        self, content: str, skill_name: str, source_path: str = ""
    ) -> list[SkillChunk]:
        """
        将单个 SKILL.md 切分为多个 SkillChunk

        Args:
            content: SKILL.md 完整内容（含 frontmatter）
            skill_name: Skill 名称
            source_path: 文件路径

        Returns:
            SkillChunk 列表
        """
        body = content
        if content.startswith("---"):
            parts = content.split("---", 2)
            if len(parts) >= 3:
                body = parts[2]

        headings = list(_HEADING_RE.finditer(body))

        if not headings:
            stripped = body.strip()
            if not stripped:
                return []
            return [
                SkillChunk(
                    skill_name=skill_name,
                    section_title="概述",
                    content=stripped,
                    source_path=source_path,
                )
            ]

        sections: list[tuple[int, str, str]] = []

        pre_content = body[: headings[0].start()].strip()
        if pre_content:
            sections.append((0, "概述", pre_content))

        for i, match in enumerate(headings):
            level = len(match.group(1))
            title = match.group(2).strip()
            start = match.end()
            end = headings[i + 1].start() if i + 1 < len(headings) else len(body)
            sec_body = body[start:end].strip()
            sections.append((level, title, sec_body))

        chunks: list[SkillChunk] = []
        for level, title, sec_body in sections:
            if level == 0:
                content_text = sec_body
            else:
                heading_prefix = "#" * level
                content_text = f"{heading_prefix} {title}\n\n{sec_body}"

            chunks.append(
                SkillChunk(
                    skill_name=skill_name,
                    section_title=title,
                    content=content_text,
                    source_path=source_path,
                )
            )

        return chunks

    def chunk_all_skills(self) -> list[SkillChunk]:
        """
        加载所有 SKILL.md 并切片

        Returns:
            所有 Skill 的 SkillChunk 列表
        """
        all_chunks: list[SkillChunk] = []

        if not self.skills_dir.exists():
            logger.warning(f"[SkillChunker] Skills 目录不存在: {self.skills_dir}")
            return all_chunks

        for skill_dir in sorted(self.skills_dir.iterdir()):
            if not skill_dir.is_dir():
                continue
            skill_file = skill_dir / "SKILL.md"
            if not skill_file.exists():
                continue

            try:
                content = skill_file.read_text(encoding="utf-8")
                skill_name = skill_dir.name
                chunks = self.chunk_skill(content, skill_name, str(skill_file))
                all_chunks.extend(chunks)
                logger.info(
                    f"[SkillChunker] {skill_name}: {len(chunks)} 个切片"
                )
            except Exception as e:
                logger.error(f"[SkillChunker] 切片失败: {skill_dir}, error={e}")

        logger.info(f"[SkillChunker] 切片完成: {len(all_chunks)} 个切片")
        return all_chunks


_skill_chunker: Optional[SkillChunker] = None


def get_skill_chunker(skills_dir: str | Path = "skills") -> SkillChunker:
    """获取 SkillChunker 单例"""
    global _skill_chunker
    if _skill_chunker is None:
        _skill_chunker = SkillChunker(skills_dir)
    return _skill_chunker
