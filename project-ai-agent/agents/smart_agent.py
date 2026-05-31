"""
智能 Agent（LangGraph ReAct）

工作流：
  thinking ──→ (有 tool_calls) ──→ thinking (循环)
      │
      └──→ (无 tool_calls) ──→ answer ──→ END

架构说明：
  - thinking 节点：负责 LLM 推理（thinking 事件）+ 工具执行（tool_call 事件）
  - judge 路由：根据是否有 tool_calls 决定继续循环还是进入回答
  - answer 节点：基于对话历史重新生成最终回答（chunk 事件，流式输出）

节点边界天然区分思考与回答，无需 [ANSWER] 标记。

事件类型统一命名（与前端、Java 端保持一致）：
  - thinking: 思考分析内容
  - chunk: 回答文本片段
  - tool_call: 工具调用/执行结果
  - done: 流完成
  - error: 错误
"""

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


@dataclass
class StreamEvent:
    """
    流式事件
    
    事件类型统一命名（与前端、Java 端保持一致）：
    - thinking: 思考分析内容
    - chunk: 回答文本片段
    - tool_call: 工具调用/执行结果
    - done: 流完成
    - error: 错误
    """
    event_type: str  # "thinking" | "chunk" | "tool_call" | "done" | "error"
    content: str = ""
    tool_name: str = ""
    tool_args: dict = field(default_factory=dict)
    extra: dict = field(default_factory=dict)
    # 累积字段（仅在 done/error 事件时携带）
    accumulated_thinking: str = ""
    accumulated_response: str = ""
    tool_calls_summary: List[dict] = field(default_factory=list)
    conversation_id: str = ""

    def to_sse_dict(self) -> dict:
        """转换为 SSE 格式的字典"""
        base = {"type": self.event_type, "content": self.content}
        if self.tool_name:
            base["tool_name"] = self.tool_name
        if self.event_type == "done":
            base["conversation_id"] = self.conversation_id
            base["tool_calls"] = self.tool_calls_summary
        return base


class AgentState(TypedDict):
    """LangGraph Agent 状态"""
    messages: List[Any]
    iteration: int
    final_answer: str
    user_id: Optional[str]
    needs_more_thinking: bool  # thinking 节点执行工具后设为 True，judge 据此路由回 thinking


class SmartAgent:
    """
    智能 Agent — ReAct 范式

    thinking 节点负责推理和工具执行，所有输出作为 thinking 事件发射。
    answer 节点基于对话历史重新生成回答，作为 token 事件流式输出。
    节点边界天然区分思考与回答，无需依赖模型输出特殊标记。
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
        # 累积状态（每次调用重置）
        self._accumulated_thinking: str = ""
        self._accumulated_response: str = ""
        self._tool_calls_list: List[dict] = []

    # ==================== 图构建 ====================

    def _build_graph(self):
        """构建 Thinking→Judge→Thinking/Answer→END 循环图"""
        workflow = StateGraph(AgentState)
        workflow.add_node("thinking", self._thinking_node)
        workflow.add_node("answer", self._answer_node)
        workflow.set_entry_point("thinking")
        workflow.add_conditional_edges("thinking", self._judge, {
            "thinking": "thinking",
            "answer": "answer",
        })
        workflow.add_edge("answer", END)
        return workflow.compile(checkpointer=MemorySaver())

    # ==================== 事件发射 ====================

    async def _emit(self, event: StreamEvent):
        """将事件推送到队列"""
        if self._event_queue:
            await self._event_queue.put(event)

    # ==================== 节点函数 ====================

    async def _thinking_node(self, state: AgentState) -> dict:
        """
        思考节点：流式调用 LLM，缓存所有输出。

        - 有 tool_calls：回放缓存内容作为 thinking 事件，执行工具，继续循环
        - 无 tool_calls：丢弃缓存（那就是最终回答），交给 answer 节点重新流式生成
        """
        llm_with_tools = self.llm.bind_tools(self.tools)
        messages = state["messages"]

        full_response = None
        thinking_chunks = []  # 缓存所有输出 chunk，待判断后决定是否发射

        async for chunk in llm_with_tools.astream(messages):
            full_response = chunk if full_response is None else full_response + chunk
            if chunk.content:
                thinking_chunks.append(chunk.content)

        tool_calls = full_response.tool_calls if full_response and hasattr(full_response, "tool_calls") else []

        if tool_calls:
            # 有工具调用：回放思考过程 → 发射工具调用 → 执行工具 → 继续循环
            for content in thinking_chunks:
                self._accumulated_thinking += content
                await self._emit(StreamEvent(event_type="thinking", content=content))

            for tc in tool_calls:
                tool_call_info = {
                    "id": len(self._tool_calls_list),
                    "name": tc["name"],
                    "args": tc.get("args", {}),
                    "result": None
                }
                self._tool_calls_list.append(tool_call_info)
                self._accumulated_thinking += f"\n🔧 调用工具: {tc['name']}"
                await self._emit(StreamEvent(
                    event_type="tool_call", content=f"\n🔧 调用工具: {tc['name']}",
                    tool_name=tc["name"], tool_args=tc["args"],
                ))

            tool_messages = await self._execute_tools(tool_calls)

            thinking_content = "".join(thinking_chunks)
            new_messages = list(state["messages"]) + [
                AIMessage(content=thinking_content, tool_calls=tool_calls)
            ] + tool_messages

            return {"messages": new_messages, "iteration": state.get("iteration", 0) + 1, "needs_more_thinking": True}

        # 无 tool_calls：LLM 输出即为"答案"，丢弃缓存，交由 answer 节点重新流式生成
        return {
            "messages": state["messages"],
            "iteration": state.get("iteration", 0) + 1,
            "final_answer": "",
            "needs_more_thinking": False,
        }

    async def _execute_tools(self, tool_calls: list) -> list[ToolMessage]:
        """并发执行所有工具调用，发射结果事件，返回 ToolMessage 列表"""

        async def _execute_single(tc: dict) -> ToolMessage:
            tool_name = tc["name"]
            tool_args = tc.get("args", {})
            logger.info(f"[Tool] 执行工具: {tool_name}, args={tool_args}")

            for tool in self.tools:
                if tool.name == tool_name:
                    try:
                        result = await tool.ainvoke(tool_args)
                        # 更新 tool_calls_list 中对应工具的 result
                        for tci in self._tool_calls_list:
                            if tci["name"] == tool_name and tci["result"] is None:
                                tci["result"] = str(result)
                                break
                        await self._emit(StreamEvent(
                            event_type="tool_call",
                            content=f"\n✅ 工具执行完成: {tool_name}",
                            tool_name=tool_name,
                            extra={"result": str(result)}
                        ))
                    except Exception as e:
                        result = f"工具执行错误: {e}"
                        logger.error(f"[Tool] 工具执行失败: {tool_name}, error={e}")
                        # 更新 tool_calls_list 中对应工具的 result（标记错误）
                        for tci in self._tool_calls_list:
                            if tci["name"] == tool_name and tci["result"] is None:
                                tci["result"] = str(result)
                                break
                        await self._emit(StreamEvent(
                            event_type="tool_call",
                            content=f"\n❌ 工具执行失败: {tool_name}",
                            tool_name=tool_name,
                            extra={"result": str(result), "error": True}
                        ))
                    return ToolMessage(content=str(result), tool_call_id=tc.get("id", ""))

            return ToolMessage(content=f"未找到工具: {tool_name}", tool_call_id=tc.get("id", ""))

        return await asyncio.gather(*[_execute_single(tc) for tc in tool_calls]) if tool_calls else []

    async def _answer_node(self, state: AgentState) -> dict:
        """
        回答节点：基于对话历史（含工具结果）生成最终回答，作为 chunk 事件流式输出。

        thinking 节点的输出已作为 thinking 事件发射，
        此节点基于完整的对话历史（包括工具调用和结果）生成面向用户的最终回答。
        """
        answer_prompt = self.prompt_manager.get_answer_prompt()
        messages = list(state["messages"]) + [HumanMessage(content=answer_prompt)]

        accumulated_content = ""
        async for chunk in self.llm.astream(messages):
            if chunk.content:
                accumulated_content += chunk.content
                self._accumulated_response += chunk.content
                await self._emit(StreamEvent(event_type="chunk", content=chunk.content))

        return {"final_answer": accumulated_content}

    # ==================== 路由函数 ====================

    @staticmethod
    def _judge(state: AgentState) -> Literal["thinking", "answer"]:
        """刚执行完工具 → 继续循环 thinking，否则 → answer，超限 → answer"""
        max_iter = get_settings().max_iterations
        if state.get("iteration", 0) >= max_iter:
            logger.warning(f"[路由] 超过最大迭代次数({max_iter})，强制输出答案")
            return "answer"

        if state.get("needs_more_thinking", False):
            return "thinking"
        return "answer"

    # ==================== 流式调用入口 ====================

    async def astream_chat_with_result(
        self, message: str, user_id: str | None = None, conversation_id: str = ""
    ):
        """
        流式对话入口

        Args:
            message: 用户消息
            user_id: 用户 ID
            conversation_id: 会话 ID（用于 done 事件）

        Yields:
            StreamEvent: thinking / chunk / tool_call / done / error
        """
        logger.info(f"[LangGraph] 开始处理, message={message[:50]}..., user_id={user_id}")

        # 重置累积状态
        self._accumulated_thinking = ""
        self._accumulated_response = ""
        self._tool_calls_list = []

        self._event_queue = asyncio.Queue()

        initial_state: AgentState = {
            "messages": [
                SystemMessage(content=self.system_prompt),
                HumanMessage(content=message),
            ],
            "iteration": 0,
            "final_answer": "",
            "user_id": user_id,
            "needs_more_thinking": False,
        }

        config = {"configurable": {"thread_id": "smart_agent"}}

        if user_id:
            set_current_user_id(user_id)

        async def run_graph():
            try:
                final_state = await self.graph.ainvoke(initial_state, config=config)
                final_answer = final_state.get("final_answer", "")
                # done 事件携带完整累积结果
                await self._emit(StreamEvent(
                    event_type="done",
                    content=final_answer or "",
                    accumulated_thinking=self._accumulated_thinking,
                    accumulated_response=self._accumulated_response,
                    tool_calls_summary=self._tool_calls_list,
                    conversation_id=conversation_id,
                ))
            except Exception as e:
                logger.error(f"[LangGraph] 执行失败: {e}")
                await self._emit(StreamEvent(
                    event_type="error",
                    content=f"抱歉，处理请求时出现错误：{str(e)}",
                    accumulated_thinking=self._accumulated_thinking,
                    accumulated_response=self._accumulated_response,
                    tool_calls_summary=self._tool_calls_list,
                    conversation_id=conversation_id,
                ))

        task = asyncio.create_task(run_graph())

        try:
            while True:
                event = await self._event_queue.get()
                yield event
                if event.event_type in ("done", "error"):
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
