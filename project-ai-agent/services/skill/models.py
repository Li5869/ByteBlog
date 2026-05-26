"""
Skill 切片数据模型
文件：project-ai-agent/services/skill/models.py
"""

from pydantic import BaseModel


class SkillChunk(BaseModel):
    """Skill 文档的一个语义切片"""

    chunk_id: str
    skill_name: str
    skill_description: str
    section_title: str
    section_level: int
    content: str
    chunk_index: int
    total_chunks: int
    source_path: str
