"""
智能 Agent（基于 LangGraph StateGraph）— 纯 ReAct 范式

架构说明：
  采用 LangGraph StateGraph 构建纯 ReAct（Reasoning + Acting）循环。

工作流：
  think ──→ (有工具调用) → execute_tools ──→ think (循环) ──→ END
    │
    └──→ (无工具调用) ───────────────────────────────────────→ END

  路由逻辑由 _route_after_think 控制：
    - 有工具调用 → execute_tools（执行工具后回到 think 循环）
    - 无工具调用 → __end__（LLM 已直接给出最终答案）

  阶段职责：
    - think 节点：LLM 绑定工具，流式输出内容（reasoning 事件）。
      当需要工具时，LLM 输出工具调用；当不需要工具时，LLM 输出就是最终答案
      （以 token 事件输出供前端展示）。
    - execute_tools 节点：执行具体工具，返回结果

深度思考模式：
  开启 deep_thinking=True 时，think 节点使用原生 OpenAI 客户端（绕过 LangChain），
  确保 DeepSeek 的 reasoning_content 在消息历史中正确回传，满足 API 校验要求。
  此时 reasoning_content 为内部推理链（reasoning 事件），
  content 为最终答案（token 事件）。
"""

from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from typing import TypedDict, List, Optional, Any, Literal
from dataclasses import dataclass, field
from langchain_core.messages import HumanMessage, AIMessage, ToolMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langchain_core.utils.function_calling import convert_to_openai_tool
from tools import ALL_TOOLS
from tools.user_tool import set_current_user_id
import openai
import asyncio
from json import dumps, loads
from loguru import logger
from config.settings import get_settings
from config.prompts import get_prompt_manager


# ==================== 数据模型 ====================

@dataclass
class StreamEvent:
    """流式事件"""
    event_type: str  # "reasoning" | "token" | "tool_call" | "done"
    content: str = ""
    tool_name: str = ""
    tool_args: dict = field(default_factory=dict)
    reasoning_content: str = ""


class AgentState(TypedDict):
    """LangGraph Agent 状态"""
    messages: List[Any]
    thinking: str
    iteration: int
    final_answer: str
    deep_thinking: bool
    user_id: Optional[str]


# ==================== 主 Agent ====================

class SmartAgent:
    """
    智能 Agent — LangGraph StateGraph 实现

    使用方式：
        agent = SmartAgent()
        async for event in agent.astream_chat_with_result("用户消息", deep_thinking=True):
            if event.event_type == "reasoning":
                print("思考:", event.content)
            elif event.event_type == "token":
                print("回答:", event.content)
            elif event.event_type == "tool_call":
                print("工具:", event.tool_name)
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
        # 原生 OpenAI 客户端：深度思考模式下绕过 LangChain，
        # 由我们手动控制消息格式，确保 reasoning_content 正确回传
        self.raw_deep_client = openai.AsyncOpenAI(
            api_key=settings.openai_api_key_deepseek,
            base_url=settings.openai_base_url_deepseek,
        )

        self.tools = ALL_TOOLS
        self.prompt_manager = prompt_manager
        # 启用渐进式披露：减少系统提示词 token 消耗
        # Agent 可通过 get_skill_details 工具获取 Skill 详细信息
        self.system_prompt = prompt_manager.get_smart_agent_system_prompt(
            progressive_disclosure=True
        )
        self.deep_system_prompt = prompt_manager.get_smart_agent_system_prompt(
            deep_thinking=True,
            progressive_disclosure=True
        )
        self._event_queue: Optional[asyncio.Queue] = None
        self.graph = self._build_graph()

        # 预转换工具定义为 OpenAI 格式
        self._openai_tools = [convert_to_openai_tool(t) for t in ALL_TOOLS]

    # ==================== LangGraph 构建 ====================

    def _build_graph(self):
        """构建 Think→Execute→Think...→END 的 ReAct 循环图"""
        workflow = StateGraph(AgentState)
        workflow.add_node("think", self._think_node)
        workflow.add_node("execute_tools", self._execute_tools_node)
        workflow.set_entry_point("think")
        workflow.add_conditional_edges("think", self._route_after_think, {
            "execute_tools": "execute_tools",
            "__end__": END,
        })
        workflow.add_edge("execute_tools", "think")
        return workflow.compile(checkpointer=MemorySaver())

    # ==================== 消息转换（深度思考模式专用） ====================

    @staticmethod
    def _to_openai_messages(messages: list) -> list[dict]:
        """
        将 LangChain 消息列表转为 OpenAI API 格式的 dict 列表。

        深度思考模式下，必须保留 AIMessage 中的 reasoning_content
        字段，否则 DeepSeek API 会返回 400 错误。
        """
        result = []
        for msg in messages:
            if isinstance(msg, SystemMessage):
                result.append({"role": "system", "content": msg.content})
            elif isinstance(msg, HumanMessage):
                result.append({"role": "user", "content": msg.content})
            elif isinstance(msg, AIMessage):
                d = {"role": "assistant", "content": msg.content or ""}
                # 回传 reasoning_content（深度思考模式下必须）
                reasoning = msg.additional_kwargs.get("reasoning_content", "")
                if reasoning:
                    d["reasoning_content"] = reasoning
                # 回传 tool_calls
                if hasattr(msg, "tool_calls") and msg.tool_calls:
                    d["tool_calls"] = [
                        {
                            "id": tc["id"],
                            "type": "function",
                            "function": {
                                "name": tc["name"],
                                "arguments": dumps(tc["args"], ensure_ascii=False),
                            },
                        }
                        for tc in msg.tool_calls
                    ]
                result.append(d)
            elif isinstance(msg, ToolMessage):
                result.append({
                    "role": "tool",
                    "content": msg.content,
                    "tool_call_id": msg.tool_call_id,
                })
        return result

    # ==================== 事件发射 ====================

    async def _emit(self, event: StreamEvent):
        """将事件推送到队列（供 chat_router 消费）"""
        if self._event_queue:
            await self._event_queue.put(event)

    # ==================== 节点函数 ====================

    async def _think_node(self, state: AgentState) -> dict:
        """思考节点路由：根据是否开启深度思考，选择不同的实现"""
        if state.get("deep_thinking",False):
            return await self._think_node_deep(state)
        return await self._think_node_normal(state)

    async def _think_node_normal(self, state: AgentState) -> dict:
        """
        普通思考节点（ReAct，使用 LangChain）：

        - LLM 绑定工具，流式输出内容
        - 有工具调用 → 发射 tool_call 事件，路由到 execute_tools 继续循环
        - 无工具调用 → LLM 输出的 content 就是最终答案，路由到 __end__
        """
        llm_with_tools = self.llm.bind_tools(self.tools)
        messages = state["messages"]

        full_response = None
        accumulated_content = ""

        async for chunk in llm_with_tools.astream(messages):
            full_response = chunk if full_response is None else full_response + chunk

            if chunk.content:
                accumulated_content += chunk.content
                await self._emit(StreamEvent(
                    event_type="token",
                    content=chunk.content,
                ))

        # LangChain 模式下没有 reasoning_content，空串传参
        tool_calls = full_response.tool_calls if full_response and hasattr(full_response, "tool_calls") else []
        return await self._emit_then_build_think_result(state, accumulated_content, "", tool_calls)

    async def _think_node_deep(self, state: AgentState) -> dict:
        """
        深度思考节点（ReAct，使用原生 OpenAI 客户端）：

        使用原生 OpenAI 客户端，手动控制消息格式，确保 DeepSeek 的
        reasoning_content 在消息历史中正确回传，满足 tool calling
        过程中的 API 校验要求。

        工作流：
          1. 将 LangChain 消息转为 OpenAI 格式（保留 reasoning_content）
          2. 调用原生 API，流式捕获 reasoning_content + content + tool_calls
          3. 发射 reasoning_content 为 reasoning 事件（用户可见的推理链）
          4. 有工具调用 → 构造带 reasoning_content 的 AIMessage，路由到 execute_tools
          5. 无工具调用 → content 就是最终答案，路由到 __end__
        """
        # 转换消息为 OpenAI 格式
        openai_messages = self._to_openai_messages(state["messages"])

        # 调用原生 OpenAI 客户端
        response = await self.raw_deep_client.chat.completions.create(
            model=get_settings().model_name_deepseek,
            messages=openai_messages,
            tools=self._openai_tools,
            stream=True,
            extra_body={"thinking": {"type": "enabled"}},
        )

        accumulated_reasoning = ""
        accumulated_content = ""
        # 流式 tool_calls 按 index 聚合
        raw_tool_calls: dict[int, dict] = {}

        async for chunk in response:
            if not chunk.choices:
                continue
            delta = chunk.choices[0].delta

            # 捕获推理链（DeepSeek 原生字段，LangChain 不识别）
            reasoning = getattr(delta, "reasoning_content", None) or ""
            if reasoning:
                accumulated_reasoning += reasoning
                # 只展示真正的推理内容，不展示 content（content 是"分析结论"等格式化输出）
                await self._emit(StreamEvent(
                    event_type="reasoning",
                    content=reasoning,
                    reasoning_content=accumulated_reasoning,
                ))

            # 最终答案（ReAct 模式）：DeepSeek 思考模式下，
            # 有 tool_calls 时 content 为 null，因此看到 content 就说明无需工具，
            # 可以安全地以 token 事件实时流式输出（打字机效果）
            if delta.content:
                accumulated_content += delta.content
                await self._emit(StreamEvent(
                    event_type="token",
                    content=delta.content,
                ))

            # 捕获工具调用（流式增量聚合）
            if delta.tool_calls:
                for tc in delta.tool_calls:
                    idx = tc.index
                    if idx not in raw_tool_calls:
                        raw_tool_calls[idx] = {
                            "id": tc.id or "",
                            "function": {"name": "", "arguments": ""},
                        }
                    if tc.id:
                        raw_tool_calls[idx]["id"] = tc.id
                    if tc.function:
                        if tc.function.name:
                            raw_tool_calls[idx]["function"]["name"] = tc.function.name
                        if tc.function.arguments:
                            raw_tool_calls[idx]["function"]["arguments"] += tc.function.arguments

        # 转换为 LangChain 标准 tool_calls 格式
        tool_calls = []
        for idx in sorted(raw_tool_calls.keys()):
            tc = raw_tool_calls[idx]
            tool_calls.append({
                "id": tc["id"],
                "name": tc["function"]["name"],
                "args": loads(tc["function"]["arguments"]),
            })

        # 由共享方法统一处理 tool_call 事件发射 + AIMessage 构造 + 返回值构建
        return await self._emit_then_build_think_result(state, accumulated_content, accumulated_reasoning, tool_calls)

    # ==================== 共享方法 ====================

    async def _emit_then_build_think_result(self, state: AgentState, accumulated_content: str, accumulated_reasoning: str, tool_calls: list) -> dict:
        """
        发射 tool_call 事件 + 构造 AIMessage + 构建 think 节点返回值

        ReAct 模式的核心逻辑：
        - 有工具调用 → 发射 tool_call 事件，构造带 tool_calls 的 AIMessage，
          返回 final_answer="" 让路由继续循环
        - 无工具调用 → LLM 的 content 就是最终答案，直接设置 final_answer，
          路由检测到无工具后走向 __end__
        """
        if tool_calls:
            # 有工具调用 → 发射 tool_call 事件，继续循环
            for tc in tool_calls:
                await self._emit(StreamEvent(
                    event_type="tool_call",
                    content=f"\n🔧 调用工具: {tc['name']}",
                    tool_name=tc["name"],
                    tool_args=tc["args"],
                ))

            # 构造带 tool_calls 的 AIMessage 存入消息历史
            ai_kwargs = {"content": accumulated_content}
            ai_kwargs["tool_calls"] = tool_calls
            if accumulated_reasoning:
                ai_kwargs["additional_kwargs"] = {"reasoning_content": accumulated_reasoning}
            new_messages = list(state["messages"]) + [AIMessage(**ai_kwargs)]

            return {
                "messages": new_messages,
                "thinking": state.get("thinking", "") + "\n" + (accumulated_reasoning or accumulated_content),
                "iteration": state.get("iteration", 0) + 1,
                "final_answer": "",  # 空串 → 继续循环
            }

        # 无工具调用 → LLM 的 content 就是最终答案（ReAct 模式）
        # accumulated_reasoning 是推理链（展示用），accumulated_content 是最终回答
        final_content = accumulated_reasoning or accumulated_content
        return {
            "messages": state["messages"],  # 不追加 AIMessage，保持本轮分析结果简洁
            "thinking": state.get("thinking", "") + "\n" + (accumulated_reasoning or accumulated_content),
            "iteration": state.get("iteration", 0) + 1,
            "final_answer": accumulated_content or accumulated_reasoning,  # 非空 → 路由到 __end__
        }

    async def _execute_tools_node(self, state: AgentState) -> dict:
        """
        工具执行节点：并发执行所有工具调用

        - 取出最后一条消息中的工具调用
        - 使用 asyncio.gather 并发执行，多工具调用时互不阻塞
        - 返回结果
        """
        last_message = state["messages"][-1]
        tool_calls = last_message.tool_calls if hasattr(last_message, "tool_calls") else []

        async def _execute_single(tc: dict) -> ToolMessage:
            """执行单个工具调用"""
            tool_name = tc["name"]
            tool_args = tc.get("args", {})
            logger.info(f"[Tool] 执行工具: {tool_name}, args={tool_args}")

            for tool in self.tools:
                if tool.name == tool_name:
                    try:
                        result = await tool.ainvoke(tool_args)
                    except Exception as e:
                        result = f"工具执行错误: {e}"
                        logger.error(f"[Tool] 工具执行失败: {tool_name}, error={e}")
                    return ToolMessage(content=str(result), tool_call_id=tc.get("id", ""))

            return ToolMessage(content=f"未找到工具: {tool_name}", tool_call_id=tc.get("id", ""))

        # 并发执行所有工具调用
        results = await asyncio.gather(*[_execute_single(tc) for tc in tool_calls]) if tool_calls else []

        return {
            "messages": list(state["messages"]) + results,
        }

    # ==================== 路由函数 ====================

    @staticmethod
    def _route_after_think(state: AgentState) -> Literal["execute_tools", "__end__"]:
        """
        ReAct 路由：
        - 有工具调用 → execute_tools（继续循环）
        - 无工具调用 → __end__（LLM 已直接给出最终答案）
        - 超过最大迭代 → __end__（兜底）
        """
        max_iter = get_settings().max_iterations
        if state.get("iteration", 0) >= max_iter:
            logger.warning(f"[路由] 超过最大迭代次数({max_iter})，强制结束")
            return "__end__"

        last_message = state["messages"][-1]
        has_tools = hasattr(last_message, "tool_calls") and bool(last_message.tool_calls)
        return "execute_tools" if has_tools else "__end__"

    # ==================== 流式调用入口 ====================

    async def astream_chat_with_result(
        self, message: str, deep_thinking: bool = False, user_id: str | None = None
    ):
        """
        流式对话入口

        使用方式：
            async for event in agent.astream_chat_with_result("你好"):
                ...

        Args:
            message: 用户消息
            deep_thinking: 是否开启深度思考模式
            user_id: 当前登录用户ID（由 Java 端传入）

        Yields:
            StreamEvent
        """
        logger.info(f"[LangGraph] 开始处理, message={message[:50]}..., deep_thinking={deep_thinking}, user_id={user_id}")

        self._event_queue = asyncio.Queue()

        system_prompt = self.deep_system_prompt if deep_thinking else self.system_prompt

        initial_state: AgentState = {
            "messages": [
                SystemMessage(content=system_prompt),
                HumanMessage(content=message),
            ],
            "thinking": "",
            "iteration": 0,
            "final_answer": "",
            "deep_thinking": deep_thinking,
            "user_id": user_id,
        }

        config = {"configurable": {"thread_id": "smart_agent"}}

        # 将用户ID注入到工具上下文变量中，供 get_current_user_id 工具读取
        if user_id:
            set_current_user_id(user_id)

        async def run_graph():
            try:
                final_state = await self.graph.ainvoke(initial_state, config=config)
                final_answer = final_state.get("final_answer", "")
                # 从 graph state 中获取跨迭代累积的 thinking
                final_thinking = final_state.get("thinking", "")
                if final_answer:
                    await self._emit(StreamEvent(event_type="done", content=final_answer, reasoning_content=final_thinking))
                else:
                    await self._emit(StreamEvent(event_type="done", content=""))
            except Exception as e:
                logger.error(f"[LangGraph] 执行失败: {e}")
                await self._emit(StreamEvent(event_type="done", content=f"抱歉，处理请求时出现错误：{str(e)}"))

        task = asyncio.create_task(run_graph())

        try:
            while True:
                event = await self._event_queue.get()
                if event.event_type == "done":
                    yield event
                    break
                yield event
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
