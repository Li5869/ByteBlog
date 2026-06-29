"""
用户上下文管理

user_id 通过 LangGraph config.configurable 传递（非对话状态），
由 memory_recall_node 预取用户信息并注入系统上下文。
ContextVar 保留供 memory_tool、writing_tool 等内部工具读取 user_id。
"""

import contextvars

# 当前用户ID上下文变量（每个请求独立，协程安全）
# 供 memory_tool、writing_tool 等内部工具读取 user_id
current_user_id: contextvars.ContextVar[str] = contextvars.ContextVar(
    "current_user_id", default=None
)


def set_current_user_id(user_id: str | None) -> None:
    """
    设置当前用户ID上下文变量

    由 smart_agent 在 astream_chat_with_result 中调用，
    将用户 ID 注入到当前协程上下文中，供内部工具函数使用。

    Args:
        user_id: 用户ID字符串，为 None 表示未登录
    """
    if user_id:
        current_user_id.set(user_id)
