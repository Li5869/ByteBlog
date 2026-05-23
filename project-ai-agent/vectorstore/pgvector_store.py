"""
pgvector 向量存储
"""


from langchain_postgres import PGVectorStore, PGEngine
from langchain_core.documents import Document
from typing import List, Optional
from loguru import logger

from config.settings import get_settings


class PgVectorStore:
    """pgvector 向量存储管理（全异步）"""

    _instance = None
    _store: Optional[PGVectorStore] = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    async def initialize(self):
        """异步初始化向量存储"""
        if self._initialized:
            return

        from services.core.embedding_service import get_embedding_service

        settings = get_settings()
        self.embeddings = get_embedding_service().get_embeddings()

        connection_string = settings.database_url
        if connection_string.startswith("postgresql://"):
            connection_string = connection_string.replace("postgresql://", "postgresql+psycopg://", 1)

        try:
            self.engine = PGEngine.from_connection_string(url=connection_string)

            try:
                await self.engine.ainit_vectorstore_table(
                    table_name="blog_knowledge",
                    vector_size=settings.vector_dimension,
                )
            except Exception as table_err:
                if "already exists" in str(table_err).lower():
                    logger.info("表 blog_knowledge 已存在，跳过创建")
                else:
                    raise table_err

            self._store = await PGVectorStore.create(
                engine=self.engine,
                table_name="blog_knowledge",
                embedding_service=self.embeddings,
            )

            self._initialized = True
            logger.info("pgvector 向量存储初始化完成（异步引擎，使用 langchain_postgres）")
        except Exception as e:
            logger.error(f"pgvector 初始化失败: {e}")
            raise

    def _ensure_initialized(self):
        """确保向量存储已初始化"""
        if not self._initialized or self._store is None:
            raise RuntimeError("向量存储未初始化，请先调用 initialize() 方法")
        return self._store

    async def aadd_documents(
            self,
            documents: List[Document],
            ids: Optional[List[str]] = None
    ) -> List[str]:
        """
        异步：添加文档到向量库

        Args:
            documents: 文档列表
            ids: 文档 ID 列表（可选）

        Returns:
            添加的文档 ID 列表
        """
        logger.info(f"开始向量化入库，文档数量: {len(documents)}")
        try:
            store = self._ensure_initialized()
            result = await store.aadd_documents(documents, ids=ids)
            logger.info(f"向量化入库成功，返回 ID 数量: {len(result) if result else 0}")
            return result
        except Exception as e:
            logger.error(f"向量化入库失败: {e}")
            raise

    async def asimilarity_search(
            self,
            query: str,
            k: int = 5,
            filter: Optional[dict] = None
    ) -> List[Document]:
        """
        异步：相似度搜索

        Args:
            query: 搜索查询
            k: 返回结果数量
            filter: metadata 过滤条件，如 {"category": "project"}
        """
        store = self._ensure_initialized()
        if filter:
            return await store.asimilarity_search(query, k=k, filter=filter)
        return await store.asimilarity_search(query, k=k)


_vector_store_instance: Optional[PgVectorStore] = None


async def get_vector_store() -> PgVectorStore:
    """获取向量存储单例（异步）"""
    global _vector_store_instance
    if _vector_store_instance is None:
        _vector_store_instance = PgVectorStore()
        await _vector_store_instance.initialize()
    return _vector_store_instance
