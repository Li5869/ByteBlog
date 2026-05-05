"""
Embedding 服务
文件：project-ai-agent/services/embedding_service.py
"""

from langchain_openai import OpenAIEmbeddings
from typing import List
from functools import lru_cache
from config.settings import get_settings


@lru_cache
def get_embedding_service() -> "EmbeddingService":
    """获取 Embedding 服务单例（@lru_cache 保证单例）"""
    return EmbeddingService()


class EmbeddingService:
    """Embedding 服务"""

    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return

        settings = get_settings()
        self.embeddings = OpenAIEmbeddings(
            model=settings.embedding_model,
            base_url=settings.openai_base_url,
            api_key=settings.openai_api_key
        )
        self._initialized = True

    def embed_query(self, text: str) -> List[float]:
        """将查询文本转换为向量"""
        return self.embeddings.embed_query(text)

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        """将文档列表转换为向量列表"""
        return self.embeddings.embed_documents(texts)

    async def aembed_query(self, text: str) -> List[float]:
        """异步：将查询文本转换为向量"""
        return await self.embeddings.aembed_query(text)

    async def aembed_documents(self, texts: List[str]) -> List[List[float]]:
        """异步：将文档列表转换为向量列表"""
        return await self.embeddings.aembed_documents(texts)

    def get_embeddings(self) -> OpenAIEmbeddings:
        """获取 Embeddings 实例"""
        return self.embeddings