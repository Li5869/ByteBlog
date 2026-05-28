"""
pgvector 向量存储（多表模式）

支持多个命名 collection，每个 collection 对应一个独立的 pgvector 表：
  - blog_knowledge   → 知识库问答（已有，Parent-Child RAG）
  - skill_chunks     → Skill 切片语义检索（新增，自包含）
"""

from langchain_postgres import PGVectorStore as LangChainPGVectorStore, PGEngine
from langchain_core.documents import Document
from typing import List, Optional
from loguru import logger

from config.settings import get_settings


class PgVectorStore:
    """
    pgvector 向量存储管理（多表模式）

    支持多个命名 collection，通过 table_name 参数路由到不同表。
    所有表共享同一 PGEngine 连接和 EmbeddingService。
    """

    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    async def initialize(self):
        """异步初始化：创建 PGEngine + 初始化所有 collection 表"""
        if self._initialized:
            return

        from services.core.embedding_service import get_embedding_service

        settings = get_settings()
        self.embeddings = get_embedding_service().get_embeddings()
        self._stores: dict[str, LangChainPGVectorStore] = {}

        connection_string = settings.database_url
        if connection_string.startswith("postgresql://"):
            connection_string = connection_string.replace("postgresql://", "postgresql+psycopg://", 1)

        try:
            self.engine = PGEngine.from_connection_string(url=connection_string)

            # 初始化已有表 blog_knowledge（知识库）
            await self._create_collection("blog_knowledge", settings.vector_dimension)

            # 初始化新增表 skill_chunks（Skill 切片）
            await self._create_collection(settings.skill_vector_table, settings.vector_dimension)

            self._initialized = True
            logger.info(f"pgvector 多表初始化完成: {list(self._stores.keys())}")
        except Exception as e:
            logger.error(f"pgvector 初始化失败: {e}")
            raise

    async def _create_collection(self, table_name: str, vector_size: int):
        """创建/复用指定名称的 PGVectorStore collection"""
        if table_name in self._stores:
            return

        try:
            await self.engine.ainit_vectorstore_table(
                table_name=table_name,
                vector_size=vector_size,
            )
        except Exception as table_err:
            if "already exists" in str(table_err).lower():
                logger.info(f"表 {table_name} 已存在，跳过创建")
            else:
                raise table_err

        self._stores[table_name] = await LangChainPGVectorStore.create(
            engine=self.engine,
            table_name=table_name,
            embedding_service=self.embeddings,
        )
        logger.info(f"pgvector collection 就绪: {table_name}")

    def _ensure_initialized(self, table_name: str = "blog_knowledge"):
        """确保指定 collection 已初始化"""
        if not self._initialized or table_name not in self._stores:
            raise RuntimeError(f"向量 collection '{table_name}' 未初始化")
        return self._stores[table_name]

    def get_engine(self) -> PGEngine:
        """获取底层 PGEngine（供原始 SQL 操作使用）"""
        if not self._initialized:
            raise RuntimeError("向量存储未初始化，请先调用 initialize() 方法")
        return self.engine

    async def aadd_documents(
            self,
            documents: List[Document],
            ids: Optional[List[str]] = None,
            table_name: str = "blog_knowledge"
    ) -> List[str]:
        """
        异步添加文档到指定表（默认 blog_knowledge）

        Args:
            documents: 文档列表
            ids: 文档 ID 列表（可选）
            table_name: 目标表名

        Returns:
            添加的文档 ID 列表
        """
        logger.info(f"开始向量化入库 [{table_name}]，文档数量: {len(documents)}")
        try:
            store = self._ensure_initialized(table_name)
            result = await store.aadd_documents(documents, ids=ids)
            logger.info(f"向量化入库成功 [{table_name}]，返回 ID 数量: {len(result) if result else 0}")
            return result
        except Exception as e:
            logger.error(f"向量化入库失败 [{table_name}]: {e}")
            raise

    async def asimilarity_search(
            self,
            query: str,
            k: int = 5,
            filter: Optional[dict] = None,
            table_name: str = "blog_knowledge"
    ) -> List[Document]:
        """
        异步相似度搜索（默认 blog_knowledge）

        Args:
            query: 搜索查询
            k: 返回结果数量
            filter: metadata 过滤条件，如 {"category": "project"}
            table_name: 目标表名
        """
        store = self._ensure_initialized(table_name)
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
