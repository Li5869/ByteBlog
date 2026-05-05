"""
Agent 注册表
文件：project-ai-agent/agents/__init__.py
"""

from agents.smart_agent import SmartAgent, get_smart_agent
from agents.writing_agent import WritingAgent, get_writing_agent

__all__ = [
    "SmartAgent",
    "get_smart_agent",
    "WritingAgent",
    "get_writing_agent",
]
