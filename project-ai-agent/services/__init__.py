"""
服务注册表

├── core/         基础设施层 (LLM, Embedding, Memory)
├── store/        数据存储层 (ES, PostgreSQL, 文档切片)
└── business/     业务逻辑层 (博客API, 写作服务)
"""

from services.core.embedding_service import EmbeddingService, get_embedding_service
from services.core.memory_service import RedisMemoryService, get_memory_service, ToolCall, MessageItem
from services.core.llm_utils import structured_generate

from services.store.document_service import DocumentService, get_document_service
from services.store.parent_store import PostgresParentStore, get_parent_store
from services.store.es_article_service import ArticleESService, get_article_es_service
from services.store.es_author_service import AuthorESService, get_author_es_service

from services.business.blog_service import BlogApiService, get_blog_service
from services.business.writing import (
    WritingContentService,
    WritingQualityService,
    WritingTagService,
    WritingTaskService,
    get_writing_task_service,
)

__all__ = [
    # core
    "EmbeddingService", "get_embedding_service",
    "RedisMemoryService", "get_memory_service",
    "ToolCall", "MessageItem",
    "structured_generate",
    # store
    "DocumentService", "get_document_service",
    "PostgresParentStore", "get_parent_store",
    "ArticleESService", "get_article_es_service",
    "AuthorESService", "get_author_es_service",
    # business
    "BlogApiService", "get_blog_service",
    "WritingContentService",
    "WritingQualityService",
    "WritingTagService",
    "WritingTaskService",
    "get_writing_task_service",
]
