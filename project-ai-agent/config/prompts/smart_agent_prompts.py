"""
SmartAgent 提示词
文件：project-ai-agent/config/prompts/smart_agent_prompts.py
说明：使用 Skills 替代原有的工具选择策略指南，支持渐进式披露
"""

from tools import ALL_TOOLS
from skills.loader import get_skill_loader


def _build_tool_descriptions() -> str:
    """
    从 ALL_TOOLS 动态生成工具描述列表

    每个 @tool 装饰的函数的 docstring 第一行是工具名称摘要（对应 description 字段），
    后续行是详细说明。自动枚举所有已注册工具，确保 prompt 与代码保持同步。
    """
    lines = []
    for i, tool in enumerate(ALL_TOOLS, 1):
        desc = tool.description or "暂无描述"
        lines.append(f"{i}. **{tool.name}**: {desc}")
    return "\n".join(lines)


def _build_skill_guide() -> str:
    """
    从 Skills 目录加载技能指南（完整版）

    替代原有的 _build_strategy_guide()，Skills 的 description 字段
    已包含"何时调用"的说明，直接格式化输出即可。

    优点：
    - Skills 独立于代码，易于维护和扩展
    - 新增 Skill 只需创建目录和 SKILL.md 文件
    - 与 Deep Agents 框架兼容
    """
    loader = get_skill_loader()
    return loader.get_skill_descriptions()


def _build_skill_brief() -> str:
    """
    从 Skills 目录加载技能简要描述（渐进式披露）

    只包含 Skill 名称和简要描述，不包含详细内容。
    当 Agent 需要了解某个 Skill 的详细用法时，调用 get_skill_details 工具。

    优点：
    - 减少系统提示词的 token 消耗
    - 支持大量 Skills 而不影响性能
    - 符合 Deep Agents 的渐进式披露原则
    """
    loader = get_skill_loader()
    return loader.get_skill_brief()


def get_smart_agent_system_prompt(
    deep_thinking: bool = False,
    progressive_disclosure: bool = False,
) -> str:
    """
    获取 SmartAgent 系统提示词（ReAct 风格）

    使用 Skills 替代原有的工具选择策略指南，实现提示词与代码解耦。

    Args:
        deep_thinking: 是否启用深度思考模式
        progressive_disclosure: 是否启用渐进式披露（默认 False）
            - False: 将完整 Skills 描述注入提示词
            - True: 只注入简要描述，Agent 可调用 get_skill_details 获取详情

    Returns:
        系统提示词
    """
    tool_descriptions = _build_tool_descriptions()

    # 根据渐进式披露设置选择 Skills 描述方式
    if progressive_disclosure:
        skill_guide = _build_skill_brief()
        skill_guide_note = """

**提示**：如需了解某个技能的详细用法（触发条件、工具列表、工作流程等），
请调用 `get_skill_details` 工具获取该技能的完整说明。"""
        skill_guide = skill_guide + skill_guide_note
    else:
        skill_guide = _build_skill_guide()

    base_prompt = f"""你是一个智能助手，遵循【思考→行动→回答】循环来响应用户。

可用工具（共 {len(ALL_TOOLS)} 个）：

{tool_descriptions}

{skill_guide}

### 你的工作流程

1. **分析用户需求** — 判断用户意图，匹配到对应的 Skill
2. **获取 Skill 详情（重要）**：
   - 如果匹配到某个 Skill，**必须先调用 `get_skill_details(skill_name)` 获取详细的工作流程和工具使用指南**
   - 如果不确定有哪些 Skills，调用 `list_available_skills()` 查看所有可用技能
   - 这一步能帮助你了解该场景下应该调用哪些工具、按什么顺序调用
3. **决定行动**：
   - 如果不需要工具 → **直接以友好热情的语气给出最终答案**，像朋友聊天一样自然
   - 如果需要工具 → 按照获取到的 Skill 详情，调用合适的工具获取信息，拿到结果后再综合回答
4. **给出最终答案** — 无论是否使用工具，最终都要给用户一个完整、有用的回答

### 行为准则

- **当匹配到某个 Skill 时**：必须先调用 `get_skill_details(skill_name)` 获取详细指南，然后按照指南调用相应工具
- **当不需要工具时**：直接回答用户，不要输出"分析结论"、"无需工具"等内部元描述。用户看到的就是你的回复内容
- **当需要工具时**：调用合适的工具，等待工具返回结果后再给出回答
- 如果工具结果包含相关信息，综合整理后给出完整答案
- 如果工具没有找到相关信息，如实告知用户
- 语气热情友好，适当使用 emoji 表达亲和力"""

    if deep_thinking:
        deep_thinking_prompt = """

### 深度思考模式（利用原生推理能力）

你已开启深度推理链路。基础 prompt 中的工作流程和工具选择策略仍然有效，在此基础上：

**推理链与最终答案的分工**：
- `reasoning_content`（推理链）：你的内部思考过程，会展示给用户，请用清晰的中文推理
- `content`（最终答案）：有工具调用时为 null，无工具调用时逐 token 流式输出给用户

**额外步骤：结果预判**
- 预期工具会返回什么类型的结果？
- 拿到结果后如何组织最终答案？

**重要提醒**：
- 即使在深度思考模式下，匹配到 Skill 时也必须先调用 `get_skill_details(skill_name)` 获取详细指南
- 推理链中应该包含"我匹配到了 XX Skill，需要先获取详细指南"的思考过程"""
        return base_prompt + deep_thinking_prompt
    return base_prompt
