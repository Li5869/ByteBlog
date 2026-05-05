"""
Pydantic 数据模型
文件：project-ai-agent/models/schemas.py
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Any, Generic, TypeVar
from datetime import datetime


T = TypeVar("T")


# ==================== 通用响应 ====================

class ApiResponse(BaseModel, Generic[T]):
    """统一 API 响应格式（与 Java 后端保持一致）"""
    code: int = 0
    msg: str = "success"
    data: Optional[T] = None


# ==================== 对话相关 ====================

class ChatRequest(BaseModel):
    """对话请求"""
    conversation_id: Optional[str] = Field(None, description="会话ID，首次对话为空")
    message: str = Field(..., description="用户消息", min_length=1, max_length=4000)
    deep_thinking: bool = Field(False, description="是否开启深度思考模式")
    user_id: Optional[str] = Field(None, description="当前登录用户ID（由Java端传入）")

class ChatResponse(BaseModel):
    """对话响应"""
    conversation_id: str = Field(..., description="会话ID")
    answer: str = Field(..., description="AI 回答")
    tools_used: Optional[List[str]] = Field(None, description="使用的工具列表")


# ==================== RAG 相关 ====================

class RAGQueryRequest(BaseModel):
    """RAG 查询请求"""
    question: str = Field(..., description="用户问题", min_length=1)
    top_k: int = Field(5, ge=1, le=20, description="返回文档数量")
    session_id: Optional[str] = Field(None, description="会话ID，用于多轮对话")


class Source(BaseModel):
    """知识来源"""
    article_id: int = Field(..., description="文章ID")
    title: str = Field(..., description="文章标题")
    relevance_score: float = Field(..., description="相关度分数")


class RAGQueryResponse(BaseModel):
    """RAG 查询响应"""
    answer: str = Field(..., description="AI 回答")
    sources: List[Source] = Field(default_factory=list, description="知识来源")
    confidence: float = Field(..., ge=0, le=1, description="置信度")


# ==================== Agent 相关 ====================

class ArticleSyncRequest(BaseModel):
    """文章同步请求"""
    id: int = Field(..., description="文章ID")
    title: str = Field(..., description="标题")
    content: str = Field(..., description="内容")
    summary: Optional[str] = Field(None, description="摘要")
    author_id: Optional[int] = Field(None, description="作者ID")
    category_id: Optional[int] = Field(None, description="分类ID")
    tags: Optional[List[str]] = Field(None, description="标签")
