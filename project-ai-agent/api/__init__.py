"""
API 路由注册表
文件：project-ai-agent/api/__init__.py
"""

from api.chat_router import router as chat_router
from api.knowledge_router import router as knowledge_router
from api.skill_router import router as skill_router
from api.memory_router import router as memory_router

__all__ = [
    "chat_router",
    "knowledge_router",
    "skill_router",
    "memory_router",
]
