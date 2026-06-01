"""
智能写作 Agent 数据模型
- WritingPlan: 写作计划 DTO
- WritingResult: 写作结果 VO
- ReflectionResult: 反思结果 VO
- WritingAgentState: LangGraph 状态（类似 Java 的 FlowContext）
- StartRequest/ResumeRequest/StopRequest/FinalizeRequest/CancelRequest: API 请求 DTO
"""

from typing import List, Optional, TypedDict, NotRequired, Annotated
from operator import add
from pydantic import BaseModel, Field, ConfigDict


# ==================== 写作流程数据模型 ====================

class WritingPlan(BaseModel):
    """写作计划 DTO"""
    topic: str = Field(description="文章的核心主题，一句话概括")
    target_audience: str = Field(description="目标读者群体描述")
    key_points: List[str] = Field(description="核心要点列表，3-5个具体要点")
    writing_style: str = Field(description="写作风格，如：教程、科普、经验分享、深度分析、技术解读、随笔")
    estimated_length: str = Field(description="预计篇幅，如：短文(800字)、中文(1500字)、长文(3000字+)")
    reference_keywords: List[str] = Field(description="参考搜索关键词列表，3-5个")
    structure: List[str] = Field(description="文章章节标题列表，每个元素是一个章节标题")


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
    all_tag_names: List[str] = Field(
        default_factory=list,
        serialization_alias="allTagNames"
    )


class ReflectionResult(BaseModel):
    """反思结果 VO"""
    score: float = Field(description="综合评分，0-10分")
    completeness: float = Field(description="完整性评分，0-10分，是否覆盖所有核心要点")
    structure: float = Field(description="结构性评分，0-10分，章节是否清晰、逻辑是否严密")
    expression: float = Field(description="表达质量评分，0-10分，语言是否流畅、术语是否准确")
    practicality: float = Field(description="实用性评分，0-10分，对目标读者是否有直接帮助")
    format: float = Field(description="格式规范评分，0-10分，Markdown格式是否正确")
    strengths: List[str] = Field(description="文章具体优点列表")
    weaknesses: List[str] = Field(description="文章具体不足列表")
    suggestions: List[str] = Field(description="具体改进建议列表")
    revised_content: Optional[str] = Field(default=None, description="修订后的文章内容（可选）")


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

    parallel_outputs: NotRequired[Annotated[List[dict], add]]


class TagGenerationResponse(BaseModel):
    """标签与分类生成结果（通过 structured_generate 调用 with_structured_output 自动生成）"""
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
