"""
API 路由注册表
文件：project-ai-agent/api/__init__.py
"""

from api.chat_router import router as chat_router
from api.knowledge_router import router as knowledge_router

__all__ = [
    "chat_router",
    "knowledge_router",
]
