"""
工具注册表

工具分为三类：
1. 通用工具（DIRECT_TOOLS）：直接绑定到 Supervisor
2. Sub-Agent 工具（SUB_AGENT_TOOLS）：通过 @tool 包装的专业 Agent
3. WritingAgent 工具（WRITING_TOOLS）：异步两阶段流程
"""

from config.settings import get_settings
from tools.common_tool import get_the_time
from tools.user_tool import get_current_user_id, get_current_user_info, set_current_user_id
from tools.skill_tool import get_skill_details, list_available_skills, search_skill_guide
from tools.writing_tool import (
    writing_start,
    writing_status,
    writing_action,
    writing_result,
    writing_publish,
)

# Sub-Agent 工具
from agents.sub_agents.tools import search_agent as search_agent_tool
from agents.sub_agents.tools import knowledge_agent as knowledge_agent_tool


# ==================== 通用工具（保留在 Supervisor）====================

DIRECT_TOOLS = [
    get_the_time,
    get_current_user_id,
    get_current_user_info,
    # Skill 工具（从 SkillAgent 融合回 Supervisor）
    get_skill_details,
    list_available_skills,
    search_skill_guide,
]

# ==================== Sub-Agent 工具（@tool 包装的专业 Agent）====================

SUB_AGENT_TOOLS = [
    search_agent_tool,
    knowledge_agent_tool,
]

# ==================== WritingAgent 工具（异步两阶段流程）====================

WRITING_TOOLS = [
    writing_start,
    writing_status,
    writing_action,
    writing_result,
    writing_publish,
]
