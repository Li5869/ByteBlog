"""
向量检索工具（pgvector）
文件：project-ai-agent/tools/vector_tool.py

实现 Parent-Child RAG 查询：
1. 向量检索找到最相关的 Child Chunks
2. 从 Child metadata 提取 doc_id
3. 根据 doc_id 从 PostgreSQL 获取完整的 Parent Document
4. 返回丰富上下文的内容

支持按 category 隔离知识库
"""

from langchain_core.tools import tool
from typing import List, Optional
from loguru import logger

from vectorstore.pgvector_store import get_vector_store


@tool
async def search_knowledge_base(
    query: str,
    category: Optional[str] = None,
    top_k: int = 5
) -> List[dict]:
    """
    搜索知识库

    使用 Parent-Child RAG 策略：
    1. 向量检索匹配最相关的 Child Chunks
    2. 通过 doc_id 关联到完整的 Parent Document
    3. 返回完整的上下文内容，提升回答质量

    Args:
        query: 搜索查询
        category: 知识库分类过滤
            - 'project': 项目知识库（项目实现、系统架构、代码逻辑）
            - 'interview': 面试知识库（技术原理、底层机制、面试题）
            - None: 全库搜索
        top_k: 返回结果数量，默认5

    Returns:
        相关文档内容列表（完整的 Parent Document 上下文）
    """
    try:
        vector_store = await get_vector_store()

        filter_dict = None
        if category:
            filter_dict = {"category": category}
            logger.info(f"知识库搜索: query='{query}', category='{category}', top_k={top_k}")
        else:
            logger.info(f"知识库搜索: query='{query}', top_k={top_k} (全库)")

        child_docs = await vector_store.asimilarity_search(query, k=top_k, filter=filter_dict)

        if not child_docs:
            logger.info("知识库搜索无结果")
            return []

        # Step 2: 从 Child metadata 提取唯一的 doc_id（去重）
        doc_ids = list(set(
            doc.metadata.get("doc_id")
            for doc in child_docs
            if doc.metadata.get("doc_id")
        ))

        # 如果没有 doc_id，回退返回 Child 内容（兼容旧数据）
        if not doc_ids:
            logger.warning("Child Chunks 中未找到 doc_id，回退返回 Child 内容")
            return [
                {
                    "content": doc.page_content,
                    "title": doc.metadata.get("title", "未知来源"),
                    "article_id": doc.metadata.get("article_id"),
                    "relevance_score": 0.0
                }
                for doc in child_docs
            ]

        # Step 3: 通过 doc_id 批量获取完整的 Parent Documents
        from services.store.parent_store import get_parent_store
        parent_store = await get_parent_store()
        parent_docs = await parent_store.amget(doc_ids)

        # Step 4: 构建返回结果
        results = []
        for parent_doc in parent_docs:
            if parent_doc is None:
                continue
            results.append({
                "content": parent_doc.page_content,
                "title": parent_doc.metadata.get("title", "未知来源"),
                "article_id": parent_doc.metadata.get("article_id"),
                "relevance_score": 0.0
            })

        # 如果 Parent 查找全部未命中，回退返回 Child 内容（兼容存量数据）
        if not results:
            logger.warning(
                f"Parent Store 中未找到对应记录（{len(doc_ids)} 个 doc_id 均未命中），"
                "回退返回 Child Chunk 内容"
            )
            return [
                {
                    "content": doc.page_content,
                    "title": doc.metadata.get("title", "未知来源"),
                    "article_id": doc.metadata.get("article_id"),
                    "relevance_score": 0.0
                }
                for doc in child_docs
            ]

        logger.info(
            f"Parent-Child RAG 查询完成：检索 {len(child_docs)} 个 Child, "
            f"关联 {len(doc_ids)} 个 Parent, 返回 {len(results)} 条结果"
        )
        return results

    except Exception as e:
        logger.error(f"知识库搜索失败: {e}")
        return []
