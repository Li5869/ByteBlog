"""
ResearchAgent 数据模型
- ResearchTask: 单个研究任务
- ResearchPlan: 研究计划（含任务列表）
- ResearchReport: 研究报告
- ResearchAgentState: Agent 状态（LangGraph StateGraph）
- ReplannerOutput: Replanner 结构化输出
- SupplementaryTask: 补充任务
- StartRequest/ResumeRequest/StopRequest: API 请求 DTO
"""

from typing import List, Optional, TypedDict, NotRequired, Annotated, Literal
from operator import add
from pydantic import BaseModel, Field


# ==================== 研究流程数据模型 ====================

class ResearchTask(BaseModel):
    """单个研究任务"""
    id: str = Field(description="任务唯一标识")
    description: str = Field(description="任务描述")
    depends_on: List[str] = Field(default_factory=list, description="依赖的任务 ID 列表")
    status: Literal["pending", "executing", "completed", "skipped", "failed"] = Field(
        default="pending", description="任务状态"
    )
    result: Optional[str] = Field(default=None, description="执行结果")
    agent_type: Literal["search", "knowledge"] = Field(
        description="执行者类型：search / knowledge（由 Planner 指定）"
    )


class ResearchPlan(BaseModel):
    """研究计划"""
    topic: str = Field(description="研究主题")
    needs_clarification: bool = Field(default=False, description="是否需要追问澄清")
    questions: List[str] = Field(default_factory=list, description="澄清问题列表")
    tasks: List[ResearchTask] = Field(default_factory=list, description="任务列表")


class ResearchReport(BaseModel):
    """研究报告"""
    topic: str = Field(description="研究主题")
    report_url: str = Field(default="", description="OSS 报告文件地址")
    content: str = Field(default="", description="完整报告内容 (Markdown)，运行时生成，不存数据库")
    key_findings: List[str] = Field(default_factory=list, description="关键发现列表")
    sources: List[dict] = Field(default_factory=list, description="引用来源")
    summary: str = Field(default="", description="报告摘要（研究面板展示用）")


class ResearchAgentState(TypedDict):
    """ResearchAgent 主状态（LangGraph 工作流上下文）

    与 WorkerState 共享的 key：
    - completed_tasks: Annotated[list, add] — Worker 通过 reducer 自动合并结果
    """
    task_id: str  # 研究任务 UUID，用于回传 Java 后端持久化报告
    user_request: str
    clarified_requirements: NotRequired[Optional[str]]
    plan: NotRequired[Optional[ResearchPlan]]
    plan_approved: NotRequired[Optional[bool]]
    user_feedback: NotRequired[Optional[str]]
    tasks: NotRequired[List[ResearchTask]]
    # completed_tasks 是 Worker 和主图的共享 key，通过 add reducer 自动合并
    # 每个 Worker 完成后将 {"task_id": ..., "result": ..., "status": ...} 写入此字段
    completed_tasks: Annotated[List[dict], add]
    findings: Annotated[List[str], add]  # 关键发现，reducer 自动合并
    report: NotRequired[Optional[ResearchReport]]
    current_step: NotRequired[Optional[str]]
    current_round: NotRequired[int]
    error: NotRequired[Optional[dict]]


class WorkerState(TypedDict):
    """Worker 节点的独立状态

    通过 Send API 接收单个任务，执行完成后将结果写入 completed_tasks。
    completed_tasks 与 ResearchAgentState 共享相同的 add reducer，结果自动合并到主状态。
    """
    task: ResearchTask                     # 当前 Worker 要执行的任务
    completed_tasks: Annotated[List[dict], add]  # 与主图共享的 reducer key


# ==================== Replanner 结构化输出 ====================

class SupplementaryTask(BaseModel):
    """补充任务"""
    description: str = Field(description="任务描述")
    agent_type: Literal["search", "knowledge"] = Field(
        description="执行该任务的 Worker 类型"
    )


class ReplannerOutput(BaseModel):
    """Replanner 结构化输出"""
    stage_insight: str = Field(
        description="面向用户的阶段洞察，一句话总结当前进展和下一步方向"
    )
    decision: Literal["continue", "complete"] = Field(
        description="继续执行或生成报告"
    )
    replan_reason: str = Field(
        default="", description="补充任务的原因说明（面向用户），仅 decision=continue 时填写"
    )
    supplementary_tasks: List[SupplementaryTask] = Field(
        default_factory=list, description="补充任务列表"
    )


# ==================== Reporter 结构化输出 ====================

class ReporterOutput(BaseModel):
    """Reporter 结构化输出"""
    content: str = Field(description="完整的 Markdown 报告")
    summary: str = Field(description="报告摘要（200字以内）")
    key_findings: List[str] = Field(default_factory=list, description="关键发现列表")
    sources: List[dict] = Field(default_factory=list, description="引用来源列表")


# ==================== API 请求 DTO ====================

class StartRequest(BaseModel):
    """启动研究任务请求"""
    task_id: str = Field(..., description="任务ID（由前端生成）")
    message: str = Field(..., description="用户研究需求")


class ResumeRequest(BaseModel):
    """恢复研究任务请求"""
    task_id: str = Field(..., description="任务ID")
    response: str = Field(..., description="用户响应（回答澄清问题 / 确认计划 / 修改意见）")


class StopRequest(BaseModel):
    """停止研究任务请求"""
    task_id: str = Field(..., description="任务ID")