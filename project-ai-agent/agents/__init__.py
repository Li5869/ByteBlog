"""
Agent 注册表
文件：project-ai-agent/agents/__init__.py
"""

from agents.smart_agent import SmartAgent, get_smart_agent
from agents.writing_agent import WritingAgent, get_writing_agent
from agents.sub_agents.search_agent import SearchAgent, get_search_agent
from agents.sub_agents.knowledge_agent import KnowledgeAgent, get_knowledge_agent

__all__ = [
    "SmartAgent",
    "get_smart_agent",
    "WritingAgent",
    "get_writing_agent",
    "SearchAgent",
    "get_search_agent",
    "KnowledgeAgent",
    "get_knowledge_agent",
]
