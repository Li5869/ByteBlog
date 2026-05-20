"""
知识库管理 API
文件：project-ai-agent/api/knowledge_router.py
"""

from typing import List

from fastapi import APIRouter, HTTPException, UploadFile, File
from langchain_core.documents import Document
from loguru import logger

from models.schemas import ApiResponse, ArticleSyncRequest
from services.store.document_service import get_document_service

router = APIRouter()


@router.post("/article", response_model=ApiResponse[dict])
async def sync_article(request: ArticleSyncRequest):
    """
    同步文章到知识库

    用于博客文章发布时自动同步到向量库（Parent-Child Chunking）。
    """
    try:
        logger.info(f"同步文章到知识库: {request.id}")

        service = await get_document_service()

        full_content = f"""
标题：{request.title}

摘要：{request.summary or ''}

正文：
{request.content}
        """.strip()

        doc = Document(
            page_content=full_content,
            metadata={
                "article_id": request.id,
                "title": request.title,
                "author_id": request.author_id,
                "category_id": request.category_id,
                "tags": request.tags or [],
                "source": "article_sync"
            }
        )

        await service.parent_child_add_documents([doc])

        return ApiResponse(data={
            "article_id": request.id
        })

    except Exception as e:
        logger.error(f"同步文章失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/articles/batch", response_model=ApiResponse[dict])
async def sync_articles_batch(requests: List[ArticleSyncRequest]):
    """
    批量同步文章到知识库（Parent-Child Chunking）
    """
    try:
        logger.info(f"批量同步文章: {len(requests)} 篇")

        service = await get_document_service()
        results = {
            "total": len(requests),
            "success": 0,
            "failed": 0,
            "details": []
        }

        for article in requests:
            try:
                full_content = f"""
标题：{article.title}

摘要：{article.summary or ''}

正文：
{article.content}
                """.strip()

                doc = Document(
                    page_content=full_content,
                    metadata={
                        "article_id": article.id,
                        "title": article.title,
                        "author_id": article.author_id,
                        "category_id": article.category_id,
                        "tags": article.tags or [],
                        "source": "article_sync"
                    }
                )

                await service.parent_child_add_documents([doc])

                results["success"] += 1
                results["details"].append({
                    "article_id": article.id,
                    "status": "success"
                })
            except Exception as e:
                results["failed"] += 1
                results["details"].append({
                    "article_id": article.id,
                    "status": "failed",
                    "error": str(e)
                })

        return ApiResponse(data=results)

    except Exception as e:
        logger.error(f"批量同步文章失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/file", response_model=ApiResponse[dict])
async def upload_file(file: UploadFile = File(...)):
    """
    上传文件到知识库

    仅支持 .md 文件（Parent-Child Chunking）。
    返回 parent_ids 和 chunk_count 供 Java 端持久化。
    """
    try:
        if not file.filename:
            raise HTTPException(status_code=400, detail="文件名不能为空")

        suffix = file.filename.split(".")[-1].lower()
        if suffix != "md":
            raise HTTPException(status_code=400, detail="仅支持 .md 格式的 Markdown 文件")

        content = await file.read()
        text = content.decode("utf-8")

        service = await get_document_service()

        doc = Document(
            page_content=text,
            metadata={
                "title": file.filename,
                "source": "file_upload"
            }
        )

        parent_ids, chunk_count = await service.parent_child_add_documents([doc])

        return ApiResponse(data={
            "filename": file.filename,
            "ids": parent_ids,
            "chunk_count": chunk_count
        })

    except Exception as e:
        logger.error(f"上传文件失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))
