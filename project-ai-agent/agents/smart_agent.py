"""
智能 Agent（LangGraph ReAct + [ANSWER] 标记）

工作流：
  think ──→ (有 tool_calls) ──→ execute_tools ──→ think (循环)
    │
    └──→ (无 tool_calls) ──→ answer ──→ END

[ANSWER] 标记：模型在准备好回答时输出此标记，框架在流式过程中实时检测，
实现 thinking → token 事件类型的动态切换，避免思考与回答内容重复。
"""

import re
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from typing import TypedDict, List, Optional, Any, Literal
from dataclasses import dataclass, field
from langchain_core.messages import HumanMessage, AIMessage, ToolMessage, SystemMessage
from langchain_openai import ChatOpenAI
from tools import ALL_TOOLS
from tools.user_tool import set_current_user_id
import asyncio
from loguru import logger
from config.settings import get_settings
from config.prompts import get_prompt_manager


ANSWER_MARKER = "[ANSWER]"


@dataclass
class StreamEvent:
    """流式事件"""
    event_type: str  # "thinking" | "token" | "tool_call" | "tool_result" | "done"
    content: str = ""
    tool_name: str = ""
    tool_args: dict = field(default_factory=dict)
    extra: dict = field(default_factory=dict)  # 额外数据


class AgentState(TypedDict):
    """LangGraph Agent 状态"""
    messages: List[Any]
    iteration: int
    final_answer: str
    user_id: Optional[str]
    answer_already_streamed: bool  # 回答是否已在 think 节点中以 token 事件流式输出


class SmartAgent:
    """
    智能 Agent — ReAct 范式 + [ANSWER] 标记

    think 节点实时检测 [ANSWER] 标记，标记前输出 thinking，标记后输出 token。
    有 tool_calls 时进入 execute_tools 继续循环，否则进入 answer 结束。
    """

    def __init__(self):
        settings = get_settings()
        prompt_manager = get_prompt_manager()

        self.llm = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            extra_body={"thinking": {"type": "disabled"}},
            temperature=0.3,
            streaming=True
        )

        self.tools = ALL_TOOLS
        self.prompt_manager = prompt_manager
        self.system_prompt = prompt_manager.get_smart_agent_system_prompt()
        self._event_queue: Optional[asyncio.Queue] = None
        self.graph = self._build_graph()

    # ==================== 图构建 ====================

    def _build_graph(self):
        """构建 Think→Execute→Think/Answer→END 循环图"""
        workflow = StateGraph(AgentState)
        workflow.add_node("think", self._think_node)
        workflow.add_node("execute_tools", self._execute_tools_node)
        workflow.add_node("answer", self._answer_node)
        workflow.set_entry_point("think")
        workflow.add_conditional_edges("think", self._route_after_think, {
            "execute_tools": "execute_tools",
            "answer": "answer",
        })
        workflow.add_edge("execute_tools", "think")
        workflow.add_edge("answer", END)
        return workflow.compile(checkpointer=MemorySaver())

    # ==================== 事件发射 ====================

    async def _emit(self, event: StreamEvent):
        """将事件推送到队列"""
        if self._event_queue:
            await self._event_queue.put(event)

    # ==================== 节点函数 ====================

    async def _think_node(self, state: AgentState) -> dict:
        """
        思考节点：流式输出思考过程，实时检测 [ANSWER] 标记切换事件类型。

        三种场景：
        - 有 tool_calls → 全部为 thinking，进入 execute_tools
        - 有 [ANSWER] 标记 → 标记前 thinking，标记后 token
        - 无标记无工具 → 全部内容作为 token 流式输出
        """
        llm_with_tools = self.llm.bind_tools(self.tools)
        messages = state["messages"]

        full_response = None
        buffer = ""
        marker_found = False
        thinking_content = ""
        answer_content = ""

        async for chunk in llm_with_tools.astream(messages):
            full_response = chunk if full_response is None else full_response + chunk
            if not chunk.content:
                continue

            if marker_found:
                answer_content += chunk.content
                await self._emit(StreamEvent(event_type="token", content=chunk.content))
            else:
                buffer += chunk.content
                idx = buffer.find(ANSWER_MARKER)
                if idx >= 0:
                    marker_found = True
                    thinking_content = buffer[:idx]
                    answer_content = buffer[idx + len(ANSWER_MARKER):]
                    buffer = ""
                    if thinking_content:
                        await self._emit(StreamEvent(event_type="thinking", content=thinking_content))
                    if answer_content:
                        await self._emit(StreamEvent(event_type="token", content=answer_content))

        tool_calls = full_response.tool_calls if full_response and hasattr(full_response, "tool_calls") else []

        if tool_calls:
            if not marker_found:
                thinking_content = buffer
                if thinking_content:
                    await self._emit(StreamEvent(event_type="thinking", content=thinking_content))
            
            for tc in tool_calls:
                await self._emit(StreamEvent(
                    event_type="tool_call", content=f"\n🔧 调用工具: {tc['name']}",
                    tool_name=tc["name"], tool_args=tc["args"],
                ))
            new_messages = list(state["messages"]) + [
                AIMessage(content=thinking_content, tool_calls=tool_calls)
            ]
            return {"messages": new_messages, "iteration": state.get("iteration", 0) + 1}

        if not marker_found:
            answer_content = buffer
            if answer_content:
                await self._emit(StreamEvent(event_type="token", content=answer_content))

        return {
            "messages": state["messages"],
            "iteration": state.get("iteration", 0) + 1,
            "final_answer": answer_content.strip(),
            "answer_already_streamed": True,
        }

    async def _execute_tools_node(self, state: AgentState) -> dict:
        """工具执行节点：并发执行所有工具调用，返回结果供 think 节点继续推理。"""
        last_message = state["messages"][-1]
        tool_calls = last_message.tool_calls if hasattr(last_message, "tool_calls") else []

        async def _execute_single(tc: dict) -> ToolMessage:
            tool_name = tc["name"]
            tool_args = tc.get("args", {})
            logger.info(f"[Tool] 执行工具: {tool_name}, args={tool_args}")

            for tool in self.tools:
                if tool.name == tool_name:
                    try:
                        result = await tool.ainvoke(tool_args)
                        # 发射工具执行结果事件
                        await self._emit(StreamEvent(
                            event_type="tool_result",
                            content=f"\n✅ 工具执行完成: {tool_name}",
                            tool_name=tool_name,
                            extra={"result": str(result)}
                        ))
                    except Exception as e:
                        result = f"工具执行错误: {e}"
                        logger.error(f"[Tool] 工具执行失败: {tool_name}, error={e}")
                        await self._emit(StreamEvent(
                            event_type="tool_result",
                            content=f"\n❌ 工具执行失败: {tool_name}",
                            tool_name=tool_name,
                            extra={"result": str(result), "error": True}
                        ))
                    return ToolMessage(content=str(result), tool_call_id=tc.get("id", ""))

            return ToolMessage(content=f"未找到工具: {tool_name}", tool_call_id=tc.get("id", ""))

        results = await asyncio.gather(*[_execute_single(tc) for tc in tool_calls]) if tool_calls else []
        return {"messages": list(state["messages"]) + results}

    async def _answer_node(self, state: AgentState) -> dict:
        """
        回答节点：根据 answer_already_streamed 决定是否需要发射 token 事件。

        - True：[ANSWER] 标记场景，内容已在 think 节点流式输出，跳过
        - False：兜底场景，内容以 thinking 发射过，需重新作为 token 发射
        - final_answer 为空：极端兜底，调用 LLM 生成
        """
        final_answer = state.get("final_answer", "")
        already_streamed = state.get("answer_already_streamed", False)

        if final_answer and already_streamed:
            logger.debug("[Answer] 已在 think 节点流式输出，跳过")
        elif final_answer:
            logger.info("[Answer] 兜底：将内容作为 token 事件发射")
            await self._emit(StreamEvent(event_type="token", content=final_answer))
        else:
            logger.warning("[Answer] final_answer 为空，执行兜底 LLM 调用")
            answer_prompt = """你是一个智能助手，请根据以下对话历史为用户提供最终答案。

对话历史：
{messages}

请提供你的最终答案："""

            messages_text = "\n".join([
                f"{msg.__class__.__name__}: {msg.content}" for msg in state["messages"]
            ])
            formatted_prompt = answer_prompt.format(messages=messages_text)

            accumulated_content = ""
            async for chunk in self.llm.astream([HumanMessage(content=formatted_prompt)]):
                if chunk.content:
                    accumulated_content += chunk.content
                    await self._emit(StreamEvent(event_type="token", content=chunk.content))
            final_answer = accumulated_content

        return {"final_answer": final_answer}

    # ==================== 路由函数 ====================

    @staticmethod
    def _route_after_think(state: AgentState) -> Literal["execute_tools", "answer"]:
        """有 tool_calls → execute_tools，否则 → answer，超限 → answer"""
        max_iter = get_settings().max_iterations
        if state.get("iteration", 0) >= max_iter:
            logger.warning(f"[路由] 超过最大迭代次数({max_iter})，强制输出答案")
            return "answer"

        last_message = state["messages"][-1]
        has_tools = hasattr(last_message, "tool_calls") and bool(last_message.tool_calls)
        return "execute_tools" if has_tools else "answer"

    # ==================== 流式调用入口 ====================

    async def astream_chat_with_result(
        self, message: str, user_id: str | None = None
    ):
        """
        流式对话入口

        Yields:
            StreamEvent: thinking / token / tool_call / done
        """
        logger.info(f"[LangGraph] 开始处理, message={message[:50]}..., user_id={user_id}")

        self._event_queue = asyncio.Queue()

        initial_state: AgentState = {
            "messages": [
                SystemMessage(content=self.system_prompt),
                HumanMessage(content=message),
            ],
            "iteration": 0,
            "final_answer": "",
            "user_id": user_id,
            "answer_already_streamed": False,
        }

        config = {"configurable": {"thread_id": "smart_agent"}}

        if user_id:
            set_current_user_id(user_id)

        async def run_graph():
            try:
                final_state = await self.graph.ainvoke(initial_state, config=config)
                final_answer = final_state.get("final_answer", "")
                await self._emit(StreamEvent(event_type="done", content=final_answer or ""))
            except Exception as e:
                logger.error(f"[LangGraph] 执行失败: {e}")
                await self._emit(StreamEvent(event_type="done", content=f"抱歉，处理请求时出现错误：{str(e)}"))

        task = asyncio.create_task(run_graph())

        try:
            while True:
                event = await self._event_queue.get()
                yield event
                if event.event_type == "done":
                    break
        finally:
            await task
            self._event_queue = None


# ==================== 单例 ====================

_smart_agent: Optional[SmartAgent] = None


def get_smart_agent() -> SmartAgent:
    """获取智能 Agent 单例"""
    global _smart_agent
    if _smart_agent is None:
        _smart_agent = SmartAgent()
    return _smart_agent
