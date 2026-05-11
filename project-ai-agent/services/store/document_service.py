"""
文档切片服务
文件：project-ai-agent/services/store/document_service.py
"""

from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from typing import List
from loguru import logger
import uuid


class DocumentService:
    """文档切片与向量化服务"""

    def __init__(self):
        self.vector_store = None

        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=450,
            chunk_overlap=50,
            separators=["\n\n", "\n", "。", "！", "？", "；", " ", ""],
            length_function=len,
            is_separator_regex=False
        )

        self.parent_text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=1500,
            chunk_overlap=200,
            separators=["\n\n", "\n", "。", "！", "？", "；", " ", ""],
            length_function=len,
            is_separator_regex=False
        )

    async def initialize(self):
        """异步初始化向量存储"""
        from vectorstore.pgvector_store import get_vector_store
        self.vector_store = await get_vector_store()

    async def parent_child_add_documents(
        self,
        documents: List[Document]
    ) -> None:
        """
        异步：使用 Parent-Child 策略批量入库

        流程：
        1. parent_splitter 分割 Parent
        2. 存储 Parent 到 knowledge_parent_chunks 表
        3. child_splitter 分割 Child
        4. Child 向量化 → blog_knowledge 表

        Args:
            documents: LangChain Document 列表
        """
        from services.store.parent_store import get_parent_store

        # Step 1: 分割 Parent
        parent_docs = self.parent_text_splitter.split_documents(documents)

        # Step 2: 存储 Parent 到关系表
        parent_store = await get_parent_store()
        parent_ids = [str(uuid.uuid4()) for _ in parent_docs]
        for i, doc in enumerate(parent_docs):
            doc.metadata["doc_id"] = parent_ids[i]

        await parent_store.amset(list(zip(parent_ids, parent_docs)))

        # Step 3: 每个 Parent 细分为 Children
        all_children: List[Document] = []
        for parent_doc in parent_docs:
            child_docs = self.text_splitter.split_documents([parent_doc])
            for child in child_docs:
                child.metadata["doc_id"] = parent_doc.metadata["doc_id"]
                child.metadata["title"] = parent_doc.metadata.get("title", "")
            all_children.extend(child_docs)

        # Step 4: Child 入库（向量化）
        child_ids = [str(uuid.uuid4()) for _ in all_children]
        await self.vector_store.aadd_documents(all_children, ids=child_ids)
        logger.info(
            f"Parent-Child 入库完成：{len(parent_ids)} 个 Parent, "
            f"{len(all_children)} 个 Child"
        )


_document_service: "DocumentService | None" = None


async def get_document_service() -> DocumentService:
    """获取文档服务单例（异步）"""
    global _document_service
    if _document_service is None:
        _document_service = DocumentService()
        await _document_service.initialize()
    return _document_service
