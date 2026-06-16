"""
提示词管理器 - 统一门面
文件：project-ai-agent/config/prompts/__init__.py

职责：
  作为 PromptManager 类的统一入口门面，所有提示词的实际内容定义在同级子模块中：
    - smart_agent_prompts.py   — SmartAgent 提示词
    - sub_agent_prompts.py     — Sub-Agent 提示词（SearchAgent, KnowledgeAgent）
    - writing_prompts.py       — WritingAgent 提示词

用法：
  from config.prompts import PromptManager, get_prompt_manager
  pm = get_prompt_manager()
  prompt = pm.get_smart_agent_system_prompt()
"""

from typing import Optional
from models.writing_models import WritingPlan
from config.prompts.smart_agent_prompts import (
    get_smart_agent_system_prompt as _get_smart_agent_system_prompt,
)
from config.prompts.sub_agent_prompts import (
    get_search_agent_system_prompt as _get_search_agent_system_prompt,
    get_knowledge_agent_system_prompt as _get_knowledge_agent_system_prompt,
    get_code_execution_agent_system_prompt as _get_code_execution_agent_system_prompt,
)
from config.prompts.writing_prompts import (
    get_writing_plan_prompt as _get_writing_plan_prompt,
    get_writing_revision_prompt as _get_writing_revision_prompt,
    get_title_generation_prompt as _get_title_generation_prompt,
    get_summary_generation_prompt as _get_summary_generation_prompt,
    get_content_generation_prompt as _get_content_generation_prompt,
    get_tags_generation_prompt as _get_tags_generation_prompt,
    get_writing_evaluation_prompt as _get_writing_evaluation_prompt,
    get_writing_revision_content_prompt as _get_writing_revision_content_prompt,
)


class PromptManager:
    """提示词管理器 - 统一门面，所有方法委托到对应子模块"""

    # ==================== SmartAgent ====================

    @staticmethod
    def get_smart_agent_system_prompt() -> str:
        """
        获取 SmartAgent 系统提示词
        定义位置：prompts/smart_agent_prompts.py
        """
        return _get_smart_agent_system_prompt()

    # ==================== Sub-Agent ====================

    @staticmethod
    def get_search_agent_system_prompt() -> str:
        """
        获取搜索专家 Agent 系统提示词
        定义位置：prompts/sub_agent_prompts.py
        """
        return _get_search_agent_system_prompt()

    @staticmethod
    def get_knowledge_agent_system_prompt() -> str:
        """
        获取知识库专家 Agent 系统提示词
        定义位置：prompts/sub_agent_prompts.py
        """
        return _get_knowledge_agent_system_prompt()

    @staticmethod
    def get_code_execution_agent_system_prompt() -> str:
        """
        获取代码执行专家 Agent 系统提示词
        定义位置：prompts/sub_agent_prompts.py
        """
        return _get_code_execution_agent_system_prompt()

    # ==================== WritingAgent ====================

    @staticmethod
    def get_writing_plan_prompt(user_request: str) -> str:
        """
        获取写作计划生成提示词
        定义位置：prompts/writing_prompts.py
        """
        return _get_writing_plan_prompt(user_request)

    @staticmethod
    def get_writing_revision_prompt(user_request: str, original_plan: Optional[WritingPlan], feedback: str) -> str:
        """
        获取写作计划修改提示词
        定义位置：prompts/writing_prompts.py
        """
        return _get_writing_revision_prompt(user_request, original_plan, feedback)

    @staticmethod
    def get_title_generation_prompt(plan: WritingPlan) -> str:
        """
        获取标题生成提示词
        定义位置：prompts/writing_prompts.py
        """
        return _get_title_generation_prompt(plan)

    @staticmethod
    def get_summary_generation_prompt(plan: WritingPlan, title: str) -> str:
        """
        获取摘要生成提示词
        定义位置：prompts/writing_prompts.py
        """
        return _get_summary_generation_prompt(plan, title)

    @staticmethod
    def get_content_generation_prompt(plan: WritingPlan, title: str, summary: str, references_str: str) -> str:
        """
        获取正文生成提示词
        定义位置：prompts/writing_prompts.py
        """
        return _get_content_generation_prompt(plan, title, summary, references_str)

    @staticmethod
    def get_tags_generation_prompt(plan: WritingPlan, categories_str: str, existing_tags_str: str = "") -> str:
        """
        获取标签和分类生成提示词
        定义位置：prompts/writing_prompts.py
        """
        return _get_tags_generation_prompt(plan, categories_str, existing_tags_str)

    @staticmethod
    def get_writing_evaluation_prompt(plan: WritingPlan, result_content: str, result_title: str, result_summary: str) -> str:
        """
        获取写作质量评估提示词
        定义位置：prompts/writing_prompts.py
        """
        return _get_writing_evaluation_prompt(plan, result_content, result_title, result_summary)

    @staticmethod
    def get_writing_revision_content_prompt(
            result_title: str, result_summary: str, result_content: str,
            reflection_score: float, weaknesses_str: str, suggestions_str: str) -> str:
        """
        获取文章内容微调提示词
        定义位置：prompts/writing_prompts.py
        """
        return _get_writing_revision_content_prompt(
            result_title, result_summary, result_content,
            reflection_score, weaknesses_str, suggestions_str,
        )


_prompt_manager: PromptManager = None


def get_prompt_manager() -> PromptManager:
    """获取提示词管理器单例"""
    global _prompt_manager
    if _prompt_manager is None:
        _prompt_manager = PromptManager()
    return _prompt_manager
