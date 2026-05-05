"""
文档切片服务
文件：project-ai-agent/services/store/document_service.py
"""

from langchain_text_splitters import (
    RecursiveCharacterTextSplitter,
    MarkdownHeaderTextSplitter
)
from langchain_core.documents import Document
from typing import List, Optional
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

        self.markdown_splitter = MarkdownHeaderTextSplitter(
            headers_to_split_on=[
                ("#", "header1"),
                ("##", "header2"),
                ("###", "header3"),
            ]
        )

    async def initialize(self):
        """异步初始化向量存储"""
        from vectorstore.pgvector_store import get_vector_store
        self.vector_store = await get_vector_store()

    def split_text(
        self,
        text: str,
        metadata: Optional[dict] = None
    ) -> List[Document]:
        """
        切片纯文本

        Args:
            text: 待切片的文本
            metadata: 元数据（会附加到每个切片）

        Returns:
            Document 列表
        """
        chunks = self.text_splitter.split_text(text)

        documents = []
        for i, chunk in enumerate(chunks):
            doc = Document(
                page_content=chunk,
                metadata={
                    **(metadata or {}),
                    "chunk_index": i,
                    "chunk_count": len(chunks)
                }
            )
            documents.append(doc)

        logger.info(f"文本切片完成：共 {len(documents)} 个切片")
        return documents

    def split_markdown(
        self,
        markdown_text: str,
        metadata: Optional[dict] = None
    ) -> List[Document]:
        """
        切片 Markdown 文档

        先按标题分割，再对每个部分进行细粒度切片。

        Args:
            markdown_text: Markdown 文本
            metadata: 元数据

        Returns:
            Document 列表
        """
        md_chunks = self.markdown_splitter.split_text(markdown_text)

        documents = []
        for md_doc in md_chunks:
            sub_chunks = self.text_splitter.split_text(md_doc.page_content)

            for i, chunk in enumerate(sub_chunks):
                doc = Document(
                    page_content=chunk,
                    metadata={
                        **(metadata or {}),
                        **md_doc.metadata,
                        "chunk_index": i
                    }
                )
                documents.append(doc)

        logger.info(f"Markdown 切片完成：共 {len(documents)} 个切片")
        return documents

    async def add_documents_to_vectorstore(
        self,
        documents: List[Document],
        ids: Optional[List[str]] = None
    ) -> List[str]:
        """
        异步：将文档添加到向量库

        Args:
            documents: Document 列表
            ids: 文档 ID 列表（可选）

        Returns:
            添加的文档 ID 列表
        """
        logger.info(f"调用向量库入库方法，文档数: {len(documents)}, ID数: {len(ids) if ids else 0}")
        result = await self.vector_store.aadd_documents(documents, ids=ids)
        logger.info(f"向量库入库返回结果: {result}")
        return result

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

    async def process_and_store_text(
            self,
            text: str,
            metadata: Optional[dict] = None,
            doc_id_prefix: str = "doc"
    ) -> List[str]:
        """
        异步：处理并存储文本（切片 + 向量化）

        Args:
            text: 原始文本
            metadata: 元数据
            doc_id_prefix: 文档 ID 前缀

        Returns:
            添加的文档 ID 列表
        """
        documents = self.split_text(text, metadata)

        ids = [str(uuid.uuid4()) for _ in range(len(documents))]

        return await self.add_documents_to_vectorstore(documents, ids=ids)

    async def process_and_store_markdown(
            self,
            markdown_text: str,
            metadata: Optional[dict] = None,
            doc_id_prefix: str = "md"
    ) -> List[str]:
        """
        异步：处理并存储 Markdown 文档

        Args:
            markdown_text: Markdown 文本
            metadata: 元数据
            doc_id_prefix: 文档 ID 前缀

        Returns:
            添加的文档 ID 列表
        """
        documents = self.split_markdown(markdown_text, metadata)

        ids = [str(uuid.uuid4()) for _ in range(len(documents))]

        return await self.add_documents_to_vectorstore(documents, ids=ids)


_document_service: Optional[DocumentService] = None


async def get_document_service() -> DocumentService:
    """获取文档服务单例（异步）"""
    global _document_service
    if _document_service is None:
        _document_service = DocumentService()
        await _document_service.initialize()
    return _document_service
