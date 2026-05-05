"""
Parent Chunk 持久化存储
文件：project-ai-agent/services/store/parent_store.py

继承 LangChain BaseStore 接口，使 ParentDocumentRetriever
能够将 Parent Documents 持久化到 PostgreSQL。
"""

import json
from typing import (
    AsyncIterator, Iterator, List, Optional, Sequence, Tuple
)
from loguru import logger

import asyncpg

from langchain_core.stores import BaseStore
from langchain_core.documents import Document

from config.settings import get_settings


class PostgresParentStore(BaseStore[str, Document]):
    """
    基于 PostgreSQL 的 Parent Document 存储

    实现 BaseStore 接口，供 ParentDocumentRetriever.docstore 使用。
    读写 knowledge_parent_chunks 表。
    """

    def __init__(self):
        self._pool: Optional[asyncpg.Pool] = None

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
    async def aget(self, key: str) -> Optional[Document]:
        async with self._pool.acquire() as conn:
            row = await conn.fetchrow(
                "SELECT content, metadata FROM knowledge_parent_chunks WHERE id = $1",
                key
            )
            if row is None:
                return None
            return Document(
                page_content=row["content"],
                metadata=row["metadata"]
            )

    async def aset(self, key: str, value: Document) -> None:
        async with self._pool.acquire() as conn:
            await conn.execute(
                """INSERT INTO knowledge_parent_chunks (id, content, metadata)
                   VALUES ($1, $2, $3::jsonb)
                   ON CONFLICT (id) DO UPDATE SET content = $2, metadata = $3::jsonb""",
                key,
                value.page_content,
                json.dumps(value.metadata, ensure_ascii=False)
            )

    async def amget(self, keys: Sequence[str]) -> List[Optional[Document]]:
        if not keys:
            return []
        async with self._pool.acquire() as conn:
            rows = await conn.fetch(
                "SELECT id, content, metadata FROM knowledge_parent_chunks WHERE id = ANY($1)",
                list(keys)
            )
            row_map = {row["id"]: row for row in rows}
            return [
                Document(
                    page_content=row_map[k]["content"],
                    metadata=row_map[k]["metadata"]
                ) if k in row_map else None
                for k in keys
            ]

    async def amset(self, key_value_pairs: Sequence[Tuple[str, Document]]) -> None:
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

    async def adelete(self, keys: Sequence[str]) -> None:
        if not keys:
            return
        async with self._pool.acquire() as conn:
            await conn.execute(
                "DELETE FROM knowledge_parent_chunks WHERE id = ANY($1)",
                list(keys)
            )

    async def ayield_keys(self, **kwargs) -> AsyncIterator[str]:
        async with self._pool.acquire() as conn:
            async for row in conn.cursor("SELECT id FROM knowledge_parent_chunks"):
                yield row["id"]

    def mget(self, keys: Sequence[str]) -> List[Optional[Document]]:
        raise NotImplementedError("请使用异步方法 amget")

    def mset(self, key_value_pairs: Sequence[Tuple[str, Document]]) -> None:
        raise NotImplementedError("请使用异步方法 amset")

    def mdelete(self, keys: Sequence[str]) -> None:
        raise NotImplementedError("请使用异步方法 adelete")

    def yield_keys(self, **kwargs) -> Iterator[str]:
        raise NotImplementedError("请使用异步方法 ayield_keys")


_parent_store: Optional[PostgresParentStore] = None


async def get_parent_store() -> PostgresParentStore:
    """获取 PostgresParentStore 单例"""
    global _parent_store
    if _parent_store is None:
        _parent_store = PostgresParentStore()
        await _parent_store.initialize()
    return _parent_store
