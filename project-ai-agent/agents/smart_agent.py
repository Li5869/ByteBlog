"""
智能 Agent（LangGraph ReAct）

工作流：
  memory_recall ──→ thinking ──→ judge ──→ (有 tool_calls) ──→ tool_executor ──→ thinking (循环)
       │                │
       │                └──→ (无 tool_calls) ──→ END
       │
       ▼
    首轮召回用户记忆，注入上下文
    后续轮次跳过（消息历史自然延续上下文）

架构说明：
  - memory_recall 节点：首轮对话时召回用户记忆，注入上下文
  - thinking 节点：负责 LLM 推理，输出 reasoning_content 和 content
    内部集成中期记忆：LLM 调用前基于 token 阈值压缩对话历史（LangMem）
  - tool_executor 节点：负责执行工具，发射工具事件
  - judge 路由：根据是否有 tool_calls 决定进入 tool_executor 还是结束

三层记忆体系：
  - 短期记忆：MemorySaver checkpointer，保持完整消息历史（会话内）
  - 中期记忆：LangMem summarize_messages，基于 token 阈值压缩旧消息（会话内）
  - 长期记忆：Mem0 LongTermMemoryService，跨会话用户画像/偏好/经历

使用 DeepSeek 官方思考模式：
  - reasoning_content：思维链（内部推理过程）→ 发射 thinking 事件
  - content：最终回答（给用户的内容）→ 发射 chunk 事件

流式输出采用 LangGraph 原生能力：
  - custom 流模式：通过 get_stream_writer() 发射自定义事件
"""

import asyncio
from dataclasses import dataclass, field
from typing import TypedDict, List, Optional, Literal, Annotated, Sequence

from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, ToolMessage, SystemMessage
from langchain_core.runnables import RunnableConfig
from langchain_deepseek import ChatDeepSeek
from langgraph.checkpoint.memory import MemorySaver
from langgraph.config import get_stream_writer
from langgraph.graph import StateGraph, END
from langgraph.graph.message import add_messages
# 中期记忆：LangMem 基于 token 阈值压缩对话历史
from langmem.short_term import summarize_messages, RunningSummary
from langsmith import traceable
from loguru import logger

from config.prompts import get_prompt_manager
from config.settings import get_settings
from services.core.long_term_memory_service import get_long_term_memory_service
from tools import DIRECT_TOOLS, SUB_AGENT_TOOLS, WRITING_TOOLS
from services.business.user_service import get_user_service
from common.user_context import set_current_user_id
from common.time_context import get_formatted_time


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
    """LangGraph Agent 状态（使用 add_messages reducer 自动管理消息追加）
    user_id 不属于对话流转状态，通过 config.configurable 传递（运行时上下文）
    """
    messages: Annotated[Sequence[BaseMessage], add_messages]
    iteration: int
    final_answer: str
    # 中期记忆：跨轮次的摘要状态，记录已摘要的消息 ID，避免重复摘要
    running_summary: Optional[RunningSummary]


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

        # Supervisor 工具列表：通用工具 + Sub-Agent 工具
        self.direct_tools = DIRECT_TOOLS
        self.sub_agent_tools = SUB_AGENT_TOOLS
        self.writing_tools = WRITING_TOOLS
        self.tools = self.direct_tools + self.sub_agent_tools + self.writing_tools
        # bind_tools 在初始化时执行一次（工具列表不变，无需每次调用时重复绑定）
        self.llm_with_tools = self.llm.bind_tools(self.tools)
        self.prompt_manager = prompt_manager
        self.system_prompt = prompt_manager.get_smart_agent_system_prompt()
        self.graph = self._build_graph()
        # 工具调用记录（跨节点共享：_tool_executor_node 写入，astream_chat_with_result 读取）
        self._tool_calls_list: List[dict] = []

        # 中期记忆：摘要专用 LLM（DeepSeek v4 flash 非思考模式，快速低成本）
        # 独立于主 LLM，不开启 thinking，不绑定 tools，仅用于生成对话摘要
        if settings.mid_term_memory_enabled:
            self.summarization_model = ChatDeepSeek(
                model=settings.model_name_deepseek,
                api_key=settings.openai_api_key_deepseek,
                streaming=False,  # 摘要不需要流式
            ).bind(max_tokens=settings.mid_term_memory_max_summary_tokens)
            self._mid_term_memory_enabled = True
            self._max_tokens = settings.mid_term_memory_max_tokens
            self._max_tokens_before_summary = settings.mid_term_memory_max_tokens_before_summary
            self._max_summary_tokens = settings.mid_term_memory_max_summary_tokens
            logger.info(
                f"[MidTermMemory] 已启用, "
                f"max_tokens={self._max_tokens}, "
                f"threshold={self._max_tokens_before_summary}, "
                f"summary_budget={self._max_summary_tokens}"
            )
        else:
            self._mid_term_memory_enabled = False
            logger.info("[MidTermMemory] 已禁用")

    # ==================== 图构建 ====================

    def _build_graph(self):
        """
        构建 MemoryRecall→Thinking→Judge→ToolExecutor→Thinking 循环图

        流程：
        1. memory_recall：首轮对话时召回用户记忆，注入上下文
        2. thinking：LLM 推理，输出 reasoning_content 和 content
        3. judge：根据 tool_calls 决定路由
        4. tool_executor：执行工具（如果有）
        5. 回到 thinking：基于工具结果继续推理
        """
        workflow = StateGraph(AgentState)
        workflow.add_node("memory_recall", self._memory_recall_node)
        workflow.add_node("thinking", self._thinking_node)
        workflow.add_node("tool_executor", self._tool_executor_node)

        # 入口改为 memory_recall
        workflow.set_entry_point("memory_recall")
        workflow.add_edge("memory_recall", "thinking")

        # thinking 节点后，根据是否有 tool_calls 决定路由
        workflow.add_conditional_edges("thinking", self._judge, {
            "tool_executor": "tool_executor",  # 有工具调用 → 执行工具
            "end": END,  # 无工具调用 → 结束
        })

        # tool_executor 执行完后，回到 thinking 继续推理
        workflow.add_edge("tool_executor", "thinking")

        return workflow.compile(checkpointer=MemorySaver())

    # ==================== 节点函数 ====================

    async def _memory_recall_node(self, state: AgentState, config: RunnableConfig) -> dict:
        """
        记忆召回节点：仅在对话首轮召回用户记忆。

        判断首轮：消息历史中 HumanMessage 数量 <= 1
        非首轮跳过：消息历史自然延续上下文，无需重复召回

        记忆上下文作为 SystemMessage 注入，供 LLM 参考。

        user_id 从 config.configurable 获取（运行时上下文，非对话状态）
        """
        messages = state.get("messages", [])
        user_id = config.get("configurable", {}).get("user_id")

        # 统计 HumanMessage 数量（排除 SystemMessage）
        human_messages = [m for m in messages if isinstance(m, HumanMessage)]

        # 非首轮或无用户信息，跳过召回
        if len(human_messages) > 1 or not user_id:
            logger.debug(f"[MemoryRecall] 跳过召回: human_messages={len(human_messages)}, user_id={user_id}")
            return {"messages": []}

        # 获取用户消息内容（用于记忆召回的查询）
        user_message = human_messages[0].content if human_messages else ""

        try:
            # 注入用户身份上下文（取代 get_current_user_info 工具调用，代码层面预取避免 LLM 工具调用开销）
            user_service = get_user_service()
            user_info = await user_service.get_user_info(user_id)

            if user_info:
                identity_text = (
                    f"[用户身份] 当前用户ID: {user_id}\n"
                    f"用户名: {user_info.username or '未设置'}\n"
                    f"昵称: {user_info.nickname or '未设置'}\n"
                    f"邮箱: {user_info.email or '未设置'}\n"
                    f"手机: {user_info.phone or '未设置'}\n"
                    f"简介: {user_info.bio or '未设置'}"
                )
            else:
                identity_text = f"[用户身份] 当前用户ID: {user_id}"

            context_messages = [SystemMessage(content=identity_text)]

            # 调用 LongTermMemoryService 召回记忆
            memory_service = get_long_term_memory_service()
            memories = await memory_service.recall_memories(
                user_id=user_id,
                query=user_message,
                top_k=5
            )

            # 格式化记忆上下文
            memory_context = self._format_memory_context(memories)
            if memory_context:
                context_messages.append(SystemMessage(content=memory_context))

            logger.info(f"[MemoryRecall] 注入用户上下文: user_id={user_id}, has_memory={bool(memory_context)}")
            return {"messages": context_messages}

        except Exception as e:
            logger.error(f"[MemoryRecall] 记忆召回失败: {e}")
            return {"messages": []}

    def _format_memory_context(self, memories: dict) -> str:
        """
        格式化记忆上下文为可读字符串。

        Args:
            memories: 按类型分组的记忆字典 {"profile": [...], "habits": [...], "episodes": [...]}

        Returns:
            格式化的记忆上下文字符串，如果无记忆则返回空字符串
        """
        profile = memories.get("profile", [])
        habits = memories.get("habits", [])
        episodes = memories.get("episodes", [])

        # 如果没有任何记忆，返回空字符串
        if not profile and not habits and not episodes:
            return ""

        context_parts = ["[用户记忆]"]

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

        return "\n".join(context_parts)

    async def _thinking_node(self, state: AgentState) -> dict:
        """
        思考节点：流式调用 LLM，实时发射 thinking 事件。

        DeepSeek 思考模式下 reasoning_content 和 content 天然分离：
        - reasoning_content → thinking 事件（通过 custom 模式发射）
        - content → 由 messages 流模式自动处理（无需手动发射）

        中期记忆集成：在 LLM 调用前，基于 token 阈值压缩对话历史。
        - 旧消息自动摘要，近期消息保持原样
        - RunningSummary 跨轮次追踪已摘要消息，避免重复摘要
        - 完整消息历史仍保存在 state["messages"]（checkpoint 用），LLM 只看压缩后的

        使用 add_messages reducer，只需返回新消息，LangGraph 自动追加。
        """
        writer = get_stream_writer()

        messages = state["messages"]
        # 状态更新字典：累积 running_summary、messages、iteration 等更新
        state_update: dict = {}

        # ===== 中期记忆：基于 token 阈值压缩对话历史 =====
        if self._mid_term_memory_enabled:
            llm_messages = await self._compress_messages(messages, state, state_update)
        else:
            llm_messages = messages

        # 注入最新时间上下文（每轮对话都需最新时间，不持久化到 state）
        llm_messages = [SystemMessage(content=f"[时间] {get_formatted_time()}")] + llm_messages

        full_response = None
        reasoning_chunks = []
        content_chunks = []

        # 使用（可能压缩后的）消息调用 LLM
        async for chunk in self.llm_with_tools.astream(llm_messages):
            full_response = chunk if full_response is None else full_response + chunk

            # reasoning_content：思维链 → 实时发射 thinking 事件
            if hasattr(chunk, 'additional_kwargs') and 'reasoning_content' in chunk.additional_kwargs:
                rc = chunk.additional_kwargs['reasoning_content']
                if rc:
                    reasoning_chunks.append(rc)
                    writer({"type": "thinking", "content": rc})

            # content 由 messages 流模式自动处理，无需手动发射
            if chunk.content:
                content_chunks.append(chunk.content)

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
            state_update["messages"] = [ai_message]
            state_update["iteration"] = state.get("iteration", 0) + 1
            return state_update

        # 无工具调用：返回最终回答（不追加消息）
        state_update["messages"] = []
        state_update["iteration"] = state.get("iteration", 0) + 1
        state_update["final_answer"] = content
        return state_update

    async def _compress_messages(
        self,
        messages: Sequence[BaseMessage],
        state: AgentState,
        state_update: dict
    ) -> Sequence[BaseMessage]:
        """
        中期记忆压缩：基于 token 阈值自动摘要旧消息。

        使用 LangMem 的 summarize_messages 函数：
        - 消息从旧到新处理，累计 token 达到阈值时触发摘要
        - 旧消息被压缩成一条摘要消息，近期消息保持原样
        - RunningSummary 记录已摘要的消息 ID，避免重复摘要（增量压缩）
        - SystemMessage 自动排除在摘要范围外

        Args:
            messages: 完整消息历史
            state: 当前图状态（读取 running_summary）
            state_update: 状态更新字典（写入新的 running_summary）

        Returns:
            压缩后的消息列表（供 LLM 调用使用），压缩失败时返回原始消息
        """
        try:
            # summarize_messages 是同步函数（内部调用 model.invoke），
            # 用 asyncio.to_thread 包装避免阻塞事件循环
            summarization_result = await asyncio.to_thread(
                summarize_messages,
                list(messages),
                running_summary=state.get("running_summary"),
                model=self.summarization_model,
                max_tokens=self._max_tokens,
                max_tokens_before_summary=self._max_tokens_before_summary,
                max_summary_tokens=self._max_summary_tokens,
            )
            # 只有当产生了新的摘要时才更新状态（避免覆盖已有摘要）
            if summarization_result.running_summary is not None:
                state_update["running_summary"] = summarization_result.running_summary
                logger.debug(
                    f"[MidTermMemory] 摘要已更新, "
                    f"summary_length={len(summarization_result.running_summary.summary)}"
                )
            return summarization_result.messages
        except Exception as e:
            logger.error(f"[MidTermMemory] 消息压缩失败，降级使用原始消息: {e}")
            return messages

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

    @traceable(
        name="smart_agent_chat",
        metadata={"agent_type": "smart_agent"},
        tags=["chat", "react", "multi-agent"]
    )
    async def astream_chat_with_result(
        self, message: str, user_id: str | None = None, conversation_id: str = ""
    ):
        """
        流式对话入口

        使用 LangGraph 原生流式能力（messages + custom 双流模式）：
        - messages 模式：直接获取 LLM token，延迟更低
        - custom 模式：获取 thinking / tool_call 等自定义事件

        LangSmith 自动追踪整个调用链：
        - LLM 调用（prompt/completion/Token 消耗/延迟）
        - 工具调用（工具名称/参数/返回结果）
        - 节点执行（节点名称/状态变更/边路由）

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
        }

        # LangGraph config：
        #   configurable → 节点函数可通过 config 访问（_memory_recall_node 读取 user_id）
        #   metadata → LangSmith 追踪标签，不重复 configurable 中已有的字段
        config = {
            "configurable": {
                "thread_id": conversation_id,
                "user_id": user_id,
            },
            "metadata": {
                "agent_type": "smart_agent",
            }
        }

        if user_id:
            set_current_user_id(user_id)

        try:
            # 使用 messages + custom 双流模式，messages 直接获取 LLM token
            async for chunk in self.graph.astream(
                initial_state,
                stream_mode=["messages", "custom"],
                version="v2",
                config=config,
            ):
                chunk_type = chunk.get("type", "")

                # messages 模式：直接获取 LLM token（延迟更低）
                if chunk_type == "messages":
                    msg, metadata = chunk["data"]
                    # 过滤掉工具执行结果，只保留 LLM 的 token
                    # 同时跳过携带 tool_calls 的 AIMessage 的 content（SystemMessage 无 tool_calls 属性，用 getattr 安全访问）：
                    #   LLM 调用工具时附带的内容是"过渡话语"，会被 final_answer 完整覆盖，
                    #   如果在 chunk 中输出，会与 done 事件的 content 重复。
                    has_tool_calls = getattr(msg, 'tool_calls', None)
                    if msg.content and not has_tool_calls and metadata.get("langgraph_node") == "thinking":
                        accumulated_response += msg.content
                        yield StreamEvent(
                            event_type="chunk",
                            content=msg.content
                        )

                # custom 模式：thinking / tool_call 等自定义事件
                elif chunk_type == "custom":
                    data = chunk["data"]
                    event_type = data.get("type", "")

                    if event_type == "thinking":
                        accumulated_thinking += data.get("content", "")
                        yield StreamEvent(
                            event_type="thinking",
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
                    elif event_type == "sub_agent_tool_call":
                        # Sub-Agent 工具调用 → 转发到前端 tool_call 块
                        agent_name = data.get("agent", "sub_agent")
                        tool_name = data.get("tool_name", "")
                        tool_args = data.get("tool_args", {})
                        accumulated_thinking += f"\n🔧 [{agent_name}] 调用工具: {tool_name}"
                        yield StreamEvent(
                            event_type="tool_call",
                            content=data.get("content", ""),
                            tool_name=f"[{agent_name}] {tool_name}",
                            tool_args=tool_args,
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
