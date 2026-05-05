"""
用户工具

提供获取当前登录用户ID的工具函数。
使用 contextvars 确保异步环境下的线程安全，
每个请求协程有独立的上下文变量。
"""

import contextvars
from langchain_core.tools import tool

# 当前用户ID上下文变量（每个请求独立，线程安全）
# 在 chat_router 中接收请求时 set，在工具函数中 get
current_user_id: contextvars.ContextVar[str] = contextvars.ContextVar(
    "current_user_id", default=None
)


def set_current_user_id(user_id: str | None) -> None:
    """
    设置当前用户ID上下文变量

    由 chat_router 在接收到 Java 端请求时调用，
    将用户 ID 注入到当前协程上下文中，供工具函数使用。

    Args:
        user_id: 用户ID字符串，为 None 表示未登录
    """
    if user_id:
        current_user_id.set(user_id)


@tool
async def get_current_user_id() -> str:
    """
    获取当前登录用户的ID

    当需要知道当前操作的用户是谁，或需要根据用户ID查询用户信息时使用此工具。
    返回当前登录用户的字符串ID，如果用户未登录则返回"未登录"。

    Returns:
        当前登录用户ID字符串，未登录时返回"未登录"
    """
    user_id = current_user_id.get()
    if user_id:
        return f"当前用户ID: {user_id}"
    return "当前用户未登录"
