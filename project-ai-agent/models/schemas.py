"""
Pydantic 数据模型
文件：project-ai-agent/models/schemas.py
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Any, Generic, TypeVar


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
    user_id: Optional[str] = Field(None, description="当前登录用户ID（由Java端传入）")

class ChatResponse(BaseModel):
    """对话响应"""
    conversation_id: str = Field(..., description="会话ID")
    answer: str = Field(..., description="AI 回答")
    tools_used: Optional[List[str]] = Field(None, description="使用的工具列表")


# ==================== Agent 相关 ====================

class UserInfoDTO(BaseModel):
    """用户信息 DTO（与 Java 后端 UserDTO 对应）"""
    id: Optional[int] = Field(None, description="用户ID")
    username: Optional[str] = Field(None, description="用户名")
    email: Optional[str] = Field(None, description="邮箱")
    phone: Optional[str] = Field(None, description="手机号")
    nickname: Optional[str] = Field(None, description="昵称")
    avatar: Optional[str] = Field(None, description="头像URL")
    bio: Optional[str] = Field(None, description="个人简介")
    gender: Optional[int] = Field(None, description="性别: 0-未知, 1-男, 2-女")

    def to_formatted_string(self) -> str:
        """格式化为人类可读的字符串"""
        parts = [
            f"用户ID: {self.id}",
            f"用户名: {self.username or '未设置'}",
            f"昵称: {self.nickname or '未设置'}",
            f"邮箱: {self.email or '未设置'}",
            f"手机: {self.phone or '未设置'}",
            f"简介: {self.bio or '未设置'}",
            f"头像: {self.avatar or '未设置'}",
        ]
        return "\n".join(parts)


# ==================== Skill 相关 ====================

class SkillChunk(BaseModel):
    """Skill 文档的一个语义切片"""

    chunk_id: str
    skill_name: str
    skill_description: str
    section_title: str
    section_level: int
    content: str
    chunk_index: int
    total_chunks: int
    source_path: str


class SkillIndexData(BaseModel):
    """Skill 索引重建响应数据（字段名与 Java SkillStatsVO 一致）"""
    totalSkills: int = Field(default=0, description="已索引的 Skill 数量")
    totalChunks: int = Field(default=0, description="切片总数")


class SkillStatusData(BaseModel):
    """Skill 索引状态响应数据（字段名与 Java SkillStatsVO 一致，额外保留 skills 供列表查询）"""
    skills: dict[str, int] = Field(default_factory=dict, description="各 Skill 的切片数量映射")
    totalSkills: int = Field(default=0, description="已索引的 Skill 数量")
    totalChunks: int = Field(default=0, description="切片总数")


# ==================== 记忆提取相关 ====================

class MemoryExtractItem(BaseModel):
    """记忆提取请求中的单个对话项"""
    conversation_id: str = Field(..., description="会话ID")
    user_id: str = Field(..., description="用户ID")


class MemoryExtractRequest(BaseModel):
    """记忆提取请求（由 Java XXL-Job 调用）"""
    conversations: List[MemoryExtractItem] = Field(..., description="待提取的对话列表")
