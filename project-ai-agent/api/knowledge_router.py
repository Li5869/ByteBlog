"""
知识库管理 API
文件：project-ai-agent/api/knowledge_router.py
"""

from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from langchain_core.documents import Document
from loguru import logger

from models.schemas import ApiResponse
from services.store.document_service import get_document_service

router = APIRouter()


@router.post("/file", response_model=ApiResponse[dict])
async def upload_file(
    file: UploadFile = File(...),
    category: str = Form(default="general")
):
    """
    上传文件到知识库

    仅支持 .md 文件（Parent-Child Chunking）。
    返回 parent_ids 和 chunk_count 供 Java 端持久化。

    Args:
        file: 上传的文件
        category: 知识库分类 (project/interview/general)
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
                "source": "file_upload",
                "category": category
            }
        )

        parent_ids, chunk_count = await service.parent_child_add_documents([doc])

        return ApiResponse(data={
            "filename": file.filename,
            "ids": parent_ids,
            "chunk_count": chunk_count,
            "category": category
        })

    except Exception as e:
        logger.error(f"上传文件失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))
