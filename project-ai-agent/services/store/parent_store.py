"""
Parent Chunk 持久化存储

将 Parent Documents 持久化到 PostgreSQL knowledge_parent_chunks 表。
当前仅实现批量读取（amget）和批量写入（amset），供 RAG Parent-Child 策略使用。
"""

import json
from typing import List, Optional, Sequence, Tuple

import asyncpg

from langchain_core.documents import Document

from config.settings import get_settings


class PostgresParentStore:
    """
    基于 PostgreSQL 的 Parent Document 存储

    读写 knowledge_parent_chunks 表，供 vector_tool 和 document_service 使用。
    """

    def __init__(self):
        self._pool: Optional[asyncpg.Pool] = None

    @staticmethod
    def _parse_metadata(raw_metadata) -> dict:
        if isinstance(raw_metadata, dict):
            return raw_metadata
        if isinstance(raw_metadata, str):
            return json.loads(raw_metadata)
        return {}

    async def initialize(self):
        """初始化数据库连接池并自动建表"""
        if self._pool is not None:
            return

        settings = get_settings()
        dsn = settings.database_url

        self._pool = await asyncpg.create_pool(
            dsn=dsn,
            min_size=2,
            max_size=10,
            command_timeout=10
        )

        async with self._pool.acquire() as conn:
            await conn.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_parent_chunks (
                    id      uuid PRIMARY KEY,
                    content text NOT NULL,
                    metadata jsonb DEFAULT '{}'::jsonb,
                    created_at timestamp(6) DEFAULT now()
                )
            """)

    async def amget(self, keys: Sequence[str]) -> List[Optional[Document]]:
        """批量获取 Parent Documents"""
        if not keys:
            return []
        async with self._pool.acquire() as conn:
            rows = await conn.fetch(
                "SELECT id, content, metadata FROM knowledge_parent_chunks WHERE id = ANY($1)",
                list(keys)
            )
            row_map = {str(row["id"]): row for row in rows}
            return [
                Document(
                    page_content=row_map[k]["content"],
                    metadata=self._parse_metadata(row_map[k]["metadata"])
                ) if k in row_map else None
                for k in keys
            ]

    async def amset(self, key_value_pairs: Sequence[Tuple[str, Document]]) -> None:
        """批量写入 Parent Documents"""
        if not key_value_pairs:
            return
        async with self._pool.acquire() as conn:
            async with conn.transaction():
                for key, doc in key_value_pairs:
                    await conn.execute(
                        """INSERT INTO knowledge_parent_chunks (id, content, metadata)
                           VALUES ($1, $2, $3::jsonb)
                           ON CONFLICT (id) DO UPDATE SET content = $2, metadata = $3::jsonb""",
                        key,
                        doc.page_content,
                        json.dumps(doc.metadata, ensure_ascii=False)
                    )


_parent_store: Optional[PostgresParentStore] = None


async def get_parent_store() -> PostgresParentStore:
    """获取 PostgresParentStore 单例"""
    global _parent_store
    if _parent_store is None:
        _parent_store = PostgresParentStore()
        await _parent_store.initialize()
    return _parent_store
