"""
知识库专家 Agent（基于 langchain.agents.create_agent）

作为 Sub-Agent 通过 @tool 注册到 Supervisor (SmartAgent)，
Supervisor 通过 LLM tool-calling 自主决定是否调用知识库专家。

使用 DeepSeek Chat 模型（非思考模式），适合知识库检索场景的精确回答。
"""

from typing import Optional, Callable
from uuid import uuid4

from langchain.agents import create_agent
from langchain_core.messages import AIMessage
from langchain_deepseek import ChatDeepSeek
from langgraph.checkpoint.memory import MemorySaver
from loguru import logger

from config.settings import get_settings
from config.prompts import get_prompt_manager
from tools.vector_tool import search_knowledge_base


class KnowledgeAgent:
    """
    知识库专家 Agent — 基于 create_agent 的 ReAct 范式

    拥有知识库检索工具，作为 Sub-Agent 被 Supervisor 调用。
    使用 DeepSeek Chat 模型（非思考模式），temperature=0.1 保证回答精确性。
    """

    def __init__(self):
        settings = get_settings()

        self.llm = ChatDeepSeek(
            model=settings.model_name_deepseek,
            api_key=settings.openai_api_key_deepseek,
            streaming=True,
            extra_body={"thinking": {"type": "disabled"}},
        )

        self.tools = [search_knowledge_base]
        self.system_prompt = get_prompt_manager().get_knowledge_agent_system_prompt()
        self.agent = create_agent(
            model=self.llm,
            tools=self.tools,
            system_prompt=self.system_prompt,
            checkpointer=MemorySaver(),
            name="knowledge_agent",
        )

    # ==================== 对外接口 ====================

    async def execute(self, task: str, stream_writer: Optional[Callable] = None) -> str:
        """
        被 Supervisor 调用的入口，返回字符串结果

        Args:
            task: 知识库查询任务描述（Supervisor 已包含足够上下文）
            stream_writer: Supervisor 的流式写入器，用于转发 Sub-Agent 内部事件到前端

        Returns:
            知识库查询结果字符串
        """
        thread_id = f"knowledge_{uuid4()}"

        try:
            final_content = ""
            async for chunk in self.agent.astream(
                {"messages": [{"role": "user", "content": task}]},
                config={"configurable": {"thread_id": thread_id}},
                stream_mode=["updates", "custom"],
                version="v2",
            ):
                chunk_type = chunk.get("type", "")

                if chunk_type == "custom" and stream_writer:
                    # 转发 Sub-Agent 的自定义事件到 Supervisor
                    stream_writer(chunk["data"])

                elif chunk_type == "updates":
                    # 从 updates 中提取工具调用事件和最终回答
                    for node_name, node_state in chunk["data"].items():
                        msgs = node_state.get("messages", [])
                        for m in msgs:
                            if isinstance(m, AIMessage) and m.tool_calls and stream_writer:
                                # 转发工具调用事件到前端思考块
                                for tc in m.tool_calls:
                                    writer_data = {
                                        "type": "sub_agent_tool_call",
                                        "content": f"调用工具: {tc['name']}",
                                        "agent": "knowledge_agent",
                                        "tool_name": tc["name"],
                                        "tool_args": tc.get("args", {}),
                                    }
                                    stream_writer(writer_data)
                            elif isinstance(m, AIMessage) and m.content and not m.tool_calls:
                                final_content = m.content

            if final_content:
                return final_content

            # 回退：获取完整状态提取最终回答
            result = await self.agent.aget_state(
                config={"configurable": {"thread_id": thread_id}}
            )
            for m in reversed(result.values.get("messages", [])):
                if isinstance(m, AIMessage) and m.content and not m.tool_calls:
                    return m.content

            return "知识库中未找到相关内容"

        except Exception as e:
            logger.error(f"[KnowledgeAgent] 执行失败: {e}")
            return f"知识库查询失败: {str(e)}"


# ==================== 单例 ====================

_knowledge_agent: Optional[KnowledgeAgent] = None


def get_knowledge_agent() -> KnowledgeAgent:
    """获取知识库专家 Agent 单例"""
    global _knowledge_agent
    if _knowledge_agent is None:
        _knowledge_agent = KnowledgeAgent()
    return _knowledge_agent
