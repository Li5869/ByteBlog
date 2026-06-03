"""
智能 Agent（LangGraph ReAct）

工作流：
  thinking ──→ judge ──→ (有 tool_calls) ──→ tool_executor ──→ thinking (循环)
      │
      └──→ (无 tool_calls) ──→ END

架构说明：
  - thinking 节点：负责 LLM 推理，输出 reasoning_content 和 content
  - tool_executor 节点：负责执行工具，发射工具事件
  - judge 路由：根据是否有 tool_calls 决定进入 tool_executor 还是结束

使用 DeepSeek 官方思考模式：
  - reasoning_content：思维链（内部推理过程）→ 发射 thinking 事件
  - content：最终回答（给用户的内容）→ 发射 chunk 事件

流式输出采用 LangGraph 原生能力：
  - custom 流模式：通过 get_stream_writer() 发射自定义事件
"""

from langgraph.graph import StateGraph, END
from langgraph.graph.message import add_messages
from langgraph.checkpoint.memory import MemorySaver
from langgraph.config import get_stream_writer
from typing import TypedDict, List, Optional, Any, Literal, Annotated, Sequence
from dataclasses import dataclass, field
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, ToolMessage, SystemMessage
from langchain_deepseek import ChatDeepSeek
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
    """LangGraph Agent 状态（使用 add_messages reducer 自动管理消息追加）"""
    messages: Annotated[Sequence[BaseMessage], add_messages]
    iteration: int
    final_answer: str
    user_id: Optional[str]


class SmartAgent:
    """
    智能 Agent — ReAct 范式

    架构：
    - thinking 节点：LLM 推理，输出 reasoning_content 和 content
    - tool_executor 节点：执行工具，发射工具事件
    - judge 路由：根据 tool_calls 决定路由

    使用 DeepSeek 官方思考模式，reasoning_content 和 content 天然分离。
    """

    def __init__(self):
        settings = get_settings()
        prompt_manager = get_prompt_manager()

        # ChatDeepSeek 开启官方思考模式：
        # - reasoning_content：思维链（内部推理过程）
        # - content：最终回答（给用户的内容）
        self.llm = ChatDeepSeek(
            model=settings.model_name_deepseek,
            api_key=settings.openai_api_key_deepseek,
            streaming=True,
            extra_body={
                "thinking": {"type": "enabled"},
                "reasoning_effort": "high",
            },
        )

        self.tools = ALL_TOOLS
        # bind_tools 在初始化时执行一次（工具列表不变，无需每次调用时重复绑定）
        self.llm_with_tools = self.llm.bind_tools(self.tools)
        self.prompt_manager = prompt_manager
        self.system_prompt = prompt_manager.get_smart_agent_system_prompt()
        self.graph = self._build_graph()
        # 工具调用记录（跨节点共享：_tool_executor_node 写入，astream_chat_with_result 读取）
        self._tool_calls_list: List[dict] = []

    # ==================== 图构建 ====================

    def _build_graph(self):
        """
        构建 Thinking→Judge→ToolExecutor→Thinking 循环图

        流程：
        1. thinking：LLM 推理，输出 reasoning_content 和 content
        2. judge：根据 tool_calls 决定路由
        3. tool_executor：执行工具（如果有）
        4. 回到 thinking：基于工具结果继续推理
        """
        workflow = StateGraph(AgentState)
        workflow.add_node("thinking", self._thinking_node)
        workflow.add_node("tool_executor", self._tool_executor_node)
        workflow.set_entry_point("thinking")

        # thinking 节点后，根据是否有 tool_calls 决定路由
        workflow.add_conditional_edges("thinking", self._judge, {
            "tool_executor": "tool_executor",  # 有工具调用 → 执行工具
            "end": END,  # 无工具调用 → 结束
        })

        # tool_executor 执行完后，回到 thinking 继续推理
        workflow.add_edge("tool_executor", "thinking")

        return workflow.compile(checkpointer=MemorySaver())

    # ==================== 节点函数 ====================

    async def _thinking_node(self, state: AgentState) -> dict:
        """
        思考节点：流式调用 LLM，实时发射 thinking/chunk 事件。

        DeepSeek 思考模式下 reasoning_content 和 content 天然分离：
        - reasoning_content → thinking 事件（思维链）
        - content → chunk 事件（最终回答）

        使用 add_messages reducer，只需返回新消息，LangGraph 自动追加。
        """
        writer = get_stream_writer()

        messages = state["messages"]
        full_response = None
        reasoning_chunks = []
        content_chunks = []

        async for chunk in self.llm_with_tools.astream(messages):
            full_response = chunk if full_response is None else full_response + chunk

            # reasoning_content：思维链 → 实时发射 thinking 事件
            if hasattr(chunk, 'additional_kwargs') and 'reasoning_content' in chunk.additional_kwargs:
                rc = chunk.additional_kwargs['reasoning_content']
                if rc:
                    reasoning_chunks.append(rc)
                    writer({"type": "thinking", "content": rc})

            # content：最终回答 → 实时发射 chunk 事件
            if chunk.content:
                content_chunks.append(chunk.content)
                writer({"type": "chunk", "content": chunk.content})

        tool_calls = full_response.tool_calls if full_response and hasattr(full_response, "tool_calls") else []
        reasoning_content = "".join(reasoning_chunks)
        content = "".join(content_chunks)

        if tool_calls:
            # 有工具调用：AIMessage 携带 tool_calls + reasoning_content
            # 符合 DeepSeek 官方要求：工具调用场景必须回传 reasoning_content
            ai_message = AIMessage(
                content=content,
                tool_calls=tool_calls,
                additional_kwargs={"reasoning_content": reasoning_content}
            )
            # add_messages reducer 会自动追加新消息
            return {
                "messages": [ai_message],
                "iteration": state.get("iteration", 0) + 1,
            }

        # 无工具调用：返回最终回答（不追加消息）
        return {
            "messages": [],
            "iteration": state.get("iteration", 0) + 1,
            "final_answer": content,
        }

    async def _tool_executor_node(self, state: AgentState) -> dict:
        """
        工具执行节点：从消息历史末尾的 AIMessage 提取 tool_calls 并执行。

        使用 add_messages reducer，只需返回新消息，LangGraph 自动追加。
        """
        writer = get_stream_writer()
        messages = state["messages"]

        # 从消息历史末尾的 AIMessage 提取 tool_calls（单一事实来源）
        last_message = messages[-1]
        tool_calls = last_message.tool_calls if isinstance(last_message, AIMessage) else []

        if not tool_calls:
            return {"messages": []}

        tool_map = {tool.name: tool for tool in self.tools}
        tool_calls_summary = self._tool_calls_list

        async def _execute_single(tc: dict) -> ToolMessage:
            """执行单个工具调用，记录结果并发射事件"""
            tool_name = tc["name"]
            tool_args = tc.get("args", {})
            logger.info(f"[Tool] 执行工具: {tool_name}, args={tool_args}")

            # 记录工具调用信息（供 done 事件的 tool_calls_summary 使用）
            call_info = {
                "id": len(tool_calls_summary),
                "name": tool_name,
                "args": tool_args,
                "result": None
            }
            tool_calls_summary.append(call_info)

            # 发射工具调用事件
            writer({"type": "tool_call", "content": f"\n🔧 调用工具: {tool_name}", "tool_name": tool_name, "tool_args": tool_args})

            tool = tool_map.get(tool_name)
            if not tool:
                call_info["result"] = f"未找到工具: {tool_name}"
                writer({"type": "tool_call", "content": f"\n❌ 未找到工具: {tool_name}", "tool_name": tool_name, "extra": {"result": call_info["result"], "error": True}})
                return ToolMessage(content=call_info["result"], tool_call_id=tc.get("id", ""))

            try:
                result = await tool.ainvoke(tool_args)
                call_info["result"] = str(result)
                writer({"type": "tool_call", "content": f"\n✅ 工具执行完成: {tool_name}", "tool_name": tool_name, "extra": {"result": str(result)}})
            except Exception as e:
                result = f"工具执行错误: {e}"
                call_info["result"] = result
                logger.error(f"[Tool] 工具执行失败: {tool_name}, error={e}")
                writer({"type": "tool_call", "content": f"\n❌ 工具执行失败: {tool_name}", "tool_name": tool_name, "extra": {"result": result, "error": True}})

            return ToolMessage(content=str(result), tool_call_id=tc.get("id", ""))

        # 并发执行所有工具调用
        tool_messages = await asyncio.gather(*[_execute_single(tc) for tc in tool_calls])

        # add_messages reducer 会自动追加新消息
        return {
            "messages": list(tool_messages),
        }

    # ==================== 路由函数 ====================

    @staticmethod
    def _judge(state: AgentState) -> Literal["tool_executor", "end"]:
        """
        路由决策：直接从消息历史判断（单一事实来源）
        - 最后一条消息是 AIMessage 且有 tool_calls → tool_executor
        - 否则 → end
        - 超过最大迭代次数 → end（强制结束）
        """
        max_iter = get_settings().max_iterations
        if state.get("iteration", 0) >= max_iter:
            logger.warning(f"[路由] 超过最大迭代次数({max_iter})，强制结束")
            return "end"

        # 直接检查消息历史末尾的 AIMessage 是否有 tool_calls
        messages = state.get("messages", [])
        if messages and isinstance(messages[-1], AIMessage) and messages[-1].tool_calls:
            return "tool_executor"

        return "end"

    # ==================== 流式调用入口 ====================

    async def astream_chat_with_result(
        self, message: str, user_id: str | None = None, conversation_id: str = ""
    ):
        """
        流式对话入口

        使用 LangGraph 原生流式能力（custom 流模式）：
        - thinking 事件：思维链（reasoning_content）
        - chunk 事件：最终回答（content）
        - tool_call 事件：工具调用/执行结果

        Args:
            message: 用户消息
            user_id: 用户 ID
            conversation_id: 会话 ID（用于 done 事件）

        Yields:
            StreamEvent: thinking / chunk / tool_call / done / error
        """
        logger.info(f"[LangGraph] 开始处理, message={message[:50]}..., user_id={user_id}")

        # 重置工具调用记录（实例变量，供 _tool_executor_node 写入）
        self._tool_calls_list = []

        # 局部累积变量（仅本次调用使用，无需作为实例变量）
        accumulated_thinking = ""
        accumulated_response = ""

        initial_state: AgentState = {
            "messages": [
                SystemMessage(content=self.system_prompt),
                HumanMessage(content=message),
            ],
            "iteration": 0,
            "final_answer": "",
            "user_id": user_id,
        }

        config = {"configurable": {"thread_id": "smart_agent"}}

        if user_id:
            set_current_user_id(user_id)

        final_answer = ""

        try:
            # 使用 LangGraph 原生流式 API，只监听 custom 流模式
            async for chunk in self.graph.astream(
                initial_state,
                stream_mode="custom",
                version="v2",
                config=config,
            ):
                data = chunk["data"]
                event_type = data.get("type", "")

                if event_type == "thinking":
                    accumulated_thinking += data.get("content", "")
                    yield StreamEvent(
                        event_type="thinking",
                        content=data.get("content", "")
                    )
                elif event_type == "chunk":
                    accumulated_response += data.get("content", "")
                    yield StreamEvent(
                        event_type="chunk",
                        content=data.get("content", "")
                    )
                elif event_type == "tool_call":
                    tool_name = data.get("tool_name", "")
                    tool_args = data.get("tool_args", {})
                    extra = data.get("extra", {})
                    accumulated_thinking += f"\n🔧 调用工具: {tool_name}"
                    yield StreamEvent(
                        event_type="tool_call",
                        content=data.get("content", ""),
                        tool_name=tool_name,
                        tool_args=tool_args,
                        extra=extra,
                    )

            # 获取最终状态以提取 final_answer
            final_state = await self.graph.aget_state(config)
            final_answer = final_state.values.get("final_answer", "")

            # done 事件携带完整累积结果
            yield StreamEvent(
                event_type="done",
                content=final_answer or "",
                accumulated_thinking=accumulated_thinking,
                accumulated_response=accumulated_response,
                tool_calls_summary=self._tool_calls_list,
                conversation_id=conversation_id,
            )

        except Exception as e:
            logger.error(f"[LangGraph] 执行失败: {e}")
            yield StreamEvent(
                event_type="error",
                content=f"抱歉，处理请求时出现错误：{str(e)}",
                accumulated_thinking=accumulated_thinking,
                accumulated_response=accumulated_response,
                tool_calls_summary=self._tool_calls_list,
                conversation_id=conversation_id,
            )


# ==================== 单例 ====================

_smart_agent: Optional[SmartAgent] = None


def get_smart_agent() -> SmartAgent:
    """获取智能 Agent 单例"""
    global _smart_agent
    if _smart_agent is None:
        _smart_agent = SmartAgent()
    return _smart_agent
