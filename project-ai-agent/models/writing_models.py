"""
智能写作 Agent 数据模型
- WritingPlan: 写作计划 DTO
- WritingResult: 写作结果 VO
- ReflectionResult: 反思结果 VO
- WritingAgentState: LangGraph 状态（类似 Java 的 FlowContext）
- StartRequest/ResumeRequest/StopRequest/FinalizeRequest/CancelRequest: API 请求 DTO
"""

from typing import List, Optional, TypedDict, NotRequired
from pydantic import BaseModel, Field, ConfigDict


# ==================== 写作流程数据模型 ====================

class WritingPlan(BaseModel):
    """写作计划 DTO"""
    topic: str
    target_audience: str
    key_points: List[str]
    writing_style: str
    estimated_length: str
    reference_keywords: List[str]
    structure: List[str]


class WritingResult(BaseModel):
    """写作结果 VO"""
    model_config = ConfigDict(populate_by_name=True)

    title: str
    summary: str
    content: str
    category_name: Optional[str] = Field(
        default=None,
        serialization_alias="categoryName"
    )
    category_id: Optional[int] = Field(
        default=None,
        serialization_alias="categoryId"
    )
    tag_names: List[str] = Field(
        default_factory=list,
        serialization_alias="tagNames"
    )
    tag_ids: List[int] = Field(
        default_factory=list,
        serialization_alias="tagIds"
    )


class ReflectionResult(BaseModel):
    """反思结果 VO"""
    score: float
    completeness: float
    structure: float
    expression: float
    practicality: float
    format: float
    strengths: List[str]
    weaknesses: List[str]
    suggestions: List[str]
    revised_content: Optional[str] = None


class WritingAgentState(TypedDict):
    """智能写作 Agent 状态（LangGraph 工作流上下文）"""
    user_request: str
    plan: NotRequired[Optional[WritingPlan]]
    plan_approved: NotRequired[Optional[bool]]
    plan_feedback: NotRequired[Optional[str]]
    current_step: NotRequired[Optional[str]]
    writing_result: NotRequired[Optional[WritingResult]]
    references: NotRequired[Optional[List[dict]]]
    reflection: NotRequired[Optional[ReflectionResult]]
    revision_count: NotRequired[int]
    article_id: NotRequired[Optional[str]]
    action: NotRequired[Optional[str]]
    cover: NotRequired[Optional[str]]
    error: NotRequired[Optional[dict]]


class TagGenerationResponse(BaseModel):
    """标签与分类生成结果（LLM 结构化输出，with_structured_output 自动注入 JSON Schema）"""
    category: str = Field(description="文章分类名称，从现有分类中选择最合适的")
    tags: List[str] = Field(description="文章标签名称列表，优先从现有标签中选择，也可推荐新标签")


# ==================== API 请求 DTO ====================

class StartRequest(BaseModel):
    """启动写作任务请求"""
    task_id: str = Field(..., description="任务ID（由Java后端创建）")
    message: str


class ResumeRequest(BaseModel):
    """恢复写作任务请求"""
    action: str = Field(..., description="approve 或 revise")
    feedback: Optional[str] = Field(None, description="修改意见（action=revise 时必填）")


class StopRequest(BaseModel):
    """停止写作任务请求"""
    save_partial: bool = True


class FinalizeRequest(BaseModel):
    """完成写作任务请求"""
    action: str = "complete"


class CancelRequest(BaseModel):
    """取消写作任务请求"""
    reason: Optional[str] = None
