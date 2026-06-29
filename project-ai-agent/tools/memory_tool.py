"""
记忆工具
文件：project-ai-agent/tools/memory_tool.py

提供记忆相关的工具：
- recall_memory：按需召回用户历史记忆
- save_memory：主动保存重要信息
"""

import re

from langchain_core.tools import tool
from loguru import logger

from services.core.long_term_memory_service import get_long_term_memory_service
from common.user_context import current_user_id


def _sanitize_memory_content(content: str) -> str:
    """
    清洗记忆内容，移除会导致 Mem0 内部 LLM extraction JSON 解析失败的字符。

    Mem0 的 extraction 流程会让 LLM 生成 JSON，中文双引号等特殊字符
    容易导致 LLM 输出畸形 JSON（如未转义的 ASCII 引号），从而使解析失败。
    将中文双引号替换为更安全的「」括号，语义不变但不会破坏 JSON 结构。
    """
    # 中文双引号 → 日式括号「」（不会干扰 JSON 解析）
    content = content.replace("\u201c", "\u300c").replace("\u201d", "\u300d")
    # 中文单引号 → 同理替换
    content = content.replace("\u2018", "\u300e").replace("\u2019", "\u300f")
    return content


@tool
async def recall_memory(query: str, top_k: int = 5) -> str:
    """
    召回用户的历史记忆。当用户询问历史、提到之前的内容、或需要个性化服务时调用。

    使用场景：
    1. 用户询问历史（"我之前说过什么？"、"我们上次讨论了什么？"）
    2. 用户提到之前的内容（"上次我们讨论的..."、"我之前提到的..."）
    3. 用户主动要求回忆（"你还记得我之前说的吗？"）
    4. 对话主题变化，需要相关历史记忆
    5. 需要个性化服务（根据用户偏好调整回答）

    Args:
        query: 查询内容，描述你想召回的记忆。例如：
            - "用户的技术偏好"
            - "之前讨论的 Spring AI 方案"
            - "用户的交互习惯"
        top_k: 返回记忆的最大数量，默认5条

    Returns:
        格式化的记忆内容字符串，包含用户画像、交互习惯、相关经历等信息。
        如果没有找到相关记忆，返回提示信息。
    """
    user_id = current_user_id.get()

    if not user_id:
        logger.warning("[recall_memory] 未找到用户信息，无法召回记忆")
        return "未找到用户信息，无法召回记忆"

    try:
        memory_service = get_long_term_memory_service()
        memories = await memory_service.recall_memories(
            user_id=user_id,
            query=query,
            top_k=top_k
        )

        # 格式化返回结果
        context_parts = []

        profile = memories.get("profile", [])
        habits = memories.get("habits", [])
        episodes = memories.get("episodes", [])

        # 如果没有任何记忆，返回提示
        if not profile and not habits and not episodes:
            logger.info(f"[recall_memory] 未找到相关记忆: user_id={user_id}, query={query}")
            return "未找到与查询相关的用户记忆"

        # 用户画像（语义记忆）
        if profile:
            profile_texts = []
            for mem in profile:
                memory_content = mem.get("memory", "") if isinstance(mem, dict) else str(mem)
                if memory_content:
                    profile_texts.append(memory_content)
            if profile_texts:
                context_parts.append(f"用户画像：{'；'.join(profile_texts)}")

        # 交互习惯（程序记忆）
        if habits:
            habits_texts = []
            for mem in habits:
                memory_content = mem.get("memory", "") if isinstance(mem, dict) else str(mem)
                if memory_content:
                    habits_texts.append(memory_content)
            if habits_texts:
                context_parts.append(f"交互习惯：{'；'.join(habits_texts)}")

        # 相关经历（情节记忆）
        if episodes:
            episodes_texts = []
            for mem in episodes:
                memory_content = mem.get("memory", "") if isinstance(mem, dict) else str(mem)
                if memory_content:
                    episodes_texts.append(memory_content)
            if episodes_texts:
                context_parts.append(f"相关经历：{'；'.join(episodes_texts)}")

        result = "[用户记忆]\n" + "\n".join(context_parts)
        logger.info(f"[recall_memory] 召回记忆成功: user_id={user_id}, query={query}, count={len(profile) + len(habits) + len(episodes)}")
        return result

    except Exception as e:
        logger.error(f"[recall_memory] 召回记忆失败: {e}")
        return f"召回记忆时发生错误：{str(e)}"


@tool
async def save_memory(content: str, memory_type: str = "auto") -> str:
    """
    保存用户记忆。当对话中出现重要信息时主动调用。

    使用场景：
    1. 用户明确要求记住（"记住这个"、"记下来"、"帮我记住"）
    2. 用户表达偏好（"我喜欢..."、"我不喜欢..."、"我偏好..."）
    3. 做出重要决策（"我决定用..."、"我选择..."、"我打算..."）
    4. 用户分享重要信息（"我的技术栈是..."、"我在做..."）
    5. 上下文很长，包含重要信息需要保存

    Args:
        content: 要保存的记忆内容。请用简洁清晰的语言描述要记住的信息。
            示例：
            - "用户偏好 Java 技术栈，喜欢简洁的代码风格"
            - "用户决定使用 Spring AI 而不是 LangChain4j"
            - "用户不喜欢太多 emoji，希望回答简洁"
        memory_type: 记忆类型，可选值：
            - "semantic"：语义记忆（用户的偏好、属性、知识背景）
            - "episodic"：情节记忆（关键对话的摘要）
            - "procedural"：程序记忆（用户的交互习惯）
            - "auto"：自动判断（默认）

    Returns:
        保存结果的提示信息
    """
    user_id = current_user_id.get()

    if not user_id:
        logger.warning("[save_memory] 未找到用户信息，无法保存记忆")
        return "未找到用户信息，无法保存记忆"

    try:
        memory_service = get_long_term_memory_service()

        # 清洗内容：移除会导致 Mem0 内部 LLM extraction JSON 解析失败的字符
        sanitized_content = _sanitize_memory_content(content)

        # 构造消息格式（模拟对话，让 Mem0 自动提取）
        messages = [
            {"role": "user", "content": sanitized_content}
        ]

        # 构造元数据
        metadata = {}
        if memory_type and memory_type != "auto":
            metadata["memory_type"] = memory_type

        # 调用 LongTermMemoryService 保存记忆
        result = await memory_service.add_memory(
            messages=messages,
            user_id=user_id,
            metadata=metadata
        )

        if result:
            # 提取保存的记忆内容
            memories = result.get("results", []) if isinstance(result, dict) else []
            if memories:
                saved_content = memories[0].get("memory", sanitized_content) if isinstance(memories[0], dict) else sanitized_content
                logger.info(f"[save_memory] 保存记忆成功: user_id={user_id}, content={saved_content}")
                return f"已成功记住：{saved_content}"
            else:
                # Mem0 返回空 results 说明 extraction 解析失败，记忆未实际存储
                logger.warning(f"[save_memory] Mem0 extraction 返回空结果，记忆未存储: user_id={user_id}, content={sanitized_content}")
                return "记忆保存失败，请稍后再试"
        else:
            logger.warning(f"[save_memory] 保存记忆返回空结果: user_id={user_id}")
            return "记忆保存失败，请稍后再试"

    except Exception as e:
        logger.error(f"[save_memory] 保存记忆失败: {e}")
        return f"保存记忆时发生错误：{str(e)}"
