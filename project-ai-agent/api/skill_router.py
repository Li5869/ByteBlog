"""
Skill 索引管理 API
文件：project-ai-agent/api/skill_router.py
"""

from fastapi import APIRouter
from loguru import logger

from models.schemas import ApiResponse, SkillIndexData, SkillStatusData
from scripts.index_skills import index_all_skills
from vectorstore.skill_vector_store import get_skill_vector_store

router = APIRouter()


@router.post("/index", response_model=ApiResponse[SkillIndexData])
async def skill_index():
    """
    重建 Skill 全量索引

    从 skills/ 目录读取所有 SKILL.md → SkillChunker 切片 → SkillVectorStore 向量化入库。
    先清空 skill_chunks 表中的旧数据，再全量写入新切片。
    """
    try:
        result = await index_all_skills(force=True)
        return ApiResponse(data=SkillIndexData(
            totalSkills=result["skill_count"],
            totalChunks=result["chunk_count"]
        ))
    except Exception as e:
        logger.error(f"[SkillRouter] 索引构建失败: {e}")
        return ApiResponse(code=500, msg=f"索引构建失败: {str(e)}", data=None)


@router.get("/status", response_model=ApiResponse[SkillStatusData])
async def skill_status():
    """
    查询 Skill 索引状态

    返回每个 Skill 的切片数量、总切片数等信息。
    """
    try:
        store = await get_skill_vector_store()
        stats = await store.get_stats()
        skills = stats["skills"]
        return ApiResponse(data=SkillStatusData(
            skills=skills,
            totalSkills=len(skills),
            totalChunks=stats["total_chunks"]
        ))
    except Exception as e:
        logger.error(f"[SkillRouter] 查询状态失败: {e}")
        return ApiResponse(
            code=500,
            msg=f"查询状态失败: {str(e)}",
            data=SkillStatusData()
        )
