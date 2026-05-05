"""
向量检索工具（pgvector）
文件：project-ai-agent/tools/vector_tool.py
"""

from langchain_core.tools import tool
from typing import List
from loguru import logger

from vectorstore.pgvector_store import get_vector_store


@tool
async def search_knowledge_base(
    query: str,
    top_k: int = 5
) -> List[dict]:
    """
    搜索知识库

    使用向量相似度搜索在知识库中查找相关内容。
    适用于用户有技术问题或需要查找特定知识点。

    Args:
        query: 搜索查询
        top_k: 返回结果数量，默认5

    Returns:
        相关文档内容列表
    """
    try:
        vector_store = await get_vector_store()
        docs = await vector_store.asimilarity_search(query, k=top_k)

        return [
            {
                "content": doc.page_content,
                "title": doc.metadata.get("title", "未知来源"),
                "article_id": doc.metadata.get("article_id"),
                "relevance_score": 0.0
            }
            for doc in docs
        ]
    except Exception as e:
        logger.error(f"知识库搜索失败: {e}")
        return []
