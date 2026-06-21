"""
Skill 切片向量存储
文件：project-ai-agent/vectorstore/skill_vector_store.py

薄封装层，委托 PgVectorStore 的 skill_chunks 表，提供 Skill 专属的向量操作。
"""

from typing import Optional, TYPE_CHECKING
import uuid

import asyncpg
from langchain_core.documents import Document
from loguru import logger

if TYPE_CHECKING:
    from services.skill.loader import SkillLoader

from config.settings import get_settings
from models.schemas import SkillChunk


class SkillVectorStore:
    """
    Skill 切片向量存储（委托 PgVectorStore 的 skill_chunks 表）

    使用独立的 pgvector 表 skill_chunks，与知识库 blog_knowledge 表隔离。
    通过 PgVectorStore 多表模式共享同一 PGEngine 和 EmbeddingService。
    """

    TABLE_NAME = "skill_chunks"

    def __init__(self):
        self._pg_vector = None
        self._initialized = False
        settings = get_settings()
        self.min_chunks_threshold = settings.skill_chunk_min_results
        self.similarity_threshold = settings.skill_similarity_threshold

    async def initialize(self):
        """初始化：获取 PgVectorStore 单例（已包含 skill_chunks 表的初始化）"""
        from vectorstore.pgvector_store import get_vector_store

        self._pg_vector = await get_vector_store()
        self._initialized = True

    async def add_skill_chunks(self, chunks: list[SkillChunk]) -> list[str]:
        """
        将 SkillChunk 列表向量化并入库

        流程：SkillChunk → langchain Document → PgVectorStore.aadd_documents
        """
        docs = []
        ids = []
        for chunk in chunks:
            doc = Document(
                page_content=chunk.content,
                metadata={
                    "chunk_id": chunk.chunk_id,
                    "skill_name": chunk.skill_name,
                    "skill_description": chunk.skill_description,
                    "section_title": chunk.section_title,
                    "section_level": chunk.section_level,
                    "chunk_index": chunk.chunk_index,
                    "total_chunks": chunk.total_chunks,
                    "source_path": chunk.source_path,
                }
            )
            docs.append(doc)
            ids.append(str(uuid.uuid4()))

        result = await self._pg_vector.aadd_documents(
            docs, ids=ids, table_name=self.TABLE_NAME
        )
        logger.info(f"[SkillVectorStore] 入库完成: {len(result)} 个切片")
        return result

    async def search_skills(
        self,
        query: str,
        skill_name: Optional[str] = None,
        top_k: int = 5
    ) -> list[SkillChunk]:
        """
        语义检索相关 Skill 切片

        Args:
            query: 用户查询文本
            skill_name: 可选，限定搜索某个特定 Skill
            top_k: 返回 Top-K 结果

        Returns:
            相关 SkillChunk 列表（按相似度降序）
        """
        filter_dict = None
        if skill_name:
            filter_dict = {"skill_name": skill_name}

        docs = await self._pg_vector.asimilarity_search(
            query, k=top_k, filter=filter_dict, table_name=self.TABLE_NAME
        )

        chunks = []
        for doc in docs:
            meta = doc.metadata
            chunks.append(SkillChunk(
                chunk_id=meta.get("chunk_id", ""),
                skill_name=meta.get("skill_name", ""),
                skill_description=meta.get("skill_description", ""),
                section_title=meta.get("section_title", ""),
                section_level=meta.get("section_level", 2),
                content=doc.page_content,
                chunk_index=meta.get("chunk_index", 0),
                total_chunks=meta.get("total_chunks", 0),
                source_path=meta.get("source_path", ""),
            ))
        return chunks

    async def search_and_assemble(
        self,
        query: str,
        skill_name: Optional[str] = None,
        top_k: int = 5,
        fallback_loader: "Optional[SkillLoader]" = None
    ) -> str:
        """
        搜索并组装最终 Skill 上下文（含三级降级逻辑）

        降级链路：
        ① 向量检索 → N 条结果
        ② N >= threshold → 返回格式化切片
           1 <= N < threshold → 返回切片 + "部分匹配，建议获取完整文档"
           N = 0 → fallback_loader.get_skill_content(skill_name) 返回完整文档
        ③ pgvector 异常 → fallback_loader 返回完整文档
        """
        try:
            chunks = await self.search_skills(query, skill_name, top_k)

            if len(chunks) >= self.min_chunks_threshold:
                # 成功路径：组装切片返回
                return self._format_chunks(chunks)

            elif len(chunks) > 0:
                # 部分匹配：返回切片 + 降级提示
                content = self._format_chunks(chunks)
                return (
                    f"⚠️ 语义检索仅匹配到 {len(chunks)} 条结果，可能不够全面。\n\n"
                    f"{content}\n\n"
                    f"建议调用 `get_skill_details('{skill_name}')` 获取完整文档。"
                )

            else:
                # 检索不到：自动降级
                if fallback_loader and skill_name:
                    logger.info(f"[SkillVectorStore] 检索无结果，降级加载完整文档: {skill_name}")
                    return fallback_loader.get_skill_content(skill_name)
                return "未找到相关的 Skill 指南内容。"

        except Exception as e:
            # pgvector 不可用：自动降级
            logger.error(f"[SkillVectorStore] 向量检索异常，降级加载: {e}")
            if fallback_loader and skill_name:
                return fallback_loader.get_skill_content(skill_name)
            return "Skill 指南检索失败，请稍后重试。"

    async def _get_conn(self) -> asyncpg.Connection:
        """获取 asyncpg 直连，每次调用建新连接，用完即关"""
        settings = get_settings()
        return await asyncpg.connect(dsn=settings.database_url)

    async def clear_all(self):
        """清空 skill_chunks 表所有数据（用于索引重建）"""
        conn = await self._get_conn()
        try:
            await conn.execute(f'DELETE FROM "{self.TABLE_NAME}"')
        finally:
            await conn.close()
        logger.info(f"[SkillVectorStore] 已清空 {self.TABLE_NAME} 表")

    async def delete_by_skill_name(self, skill_name: str):
        """按 skill_name 删除指定 Skill 的所有切片（用于单 Skill 重建）"""
        conn = await self._get_conn()
        try:
            await conn.execute(
                f"DELETE FROM \"{self.TABLE_NAME}\" WHERE langchain_metadata->>'skill_name' = $1",
                skill_name
            )
        finally:
            await conn.close()
        logger.info(f"[SkillVectorStore] 已删除 {skill_name} 的切片")

    async def get_stats(self) -> dict:
        """
        查询 Skill 索引状态

        Returns:
            {"skills": {name: count, ...}, "total_chunks": N}
        """
        conn = await self._get_conn()
        try:
            rows = await conn.fetch(
                f"SELECT langchain_metadata->>'skill_name' AS skill_name, COUNT(*) AS cnt "
                f'FROM "{self.TABLE_NAME}" '
                f"GROUP BY langchain_metadata->>'skill_name'"
            )
        finally:
            await conn.close()

        skills = {}
        total = 0
        for row in rows:
            name = row["skill_name"] or "unknown"
            count = row["cnt"]
            skills[name] = count
            total += count

        return {"skills": skills, "total_chunks": total}

    def _format_chunks(self, chunks: list[SkillChunk]) -> str:
        """格式化切片列表为 Markdown"""
        lines = []
        for c in chunks:
            lines.append(f"## {c.section_title}")
            lines.append(f"（来源: `{c.skill_name}` Skill, 第 {c.chunk_index}/{c.total_chunks} 段）")
            lines.append("")
            lines.append(c.content)
            lines.append("")
            lines.append("---")
            lines.append("")
        return "\n".join(lines)


# ==================== 单例 ====================

_skill_vector_store: Optional[SkillVectorStore] = None


async def get_skill_vector_store() -> SkillVectorStore:
    """获取 SkillVectorStore 单例（异步初始化）"""
    global _skill_vector_store
    if _skill_vector_store is None:
        _skill_vector_store = SkillVectorStore()
        await _skill_vector_store.initialize()
    return _skill_vector_store
