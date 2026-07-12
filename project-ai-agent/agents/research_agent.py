"""
ResearchAgent（LangGraph Orchestrator-Worker 模式）

深度研究 Agent，参考 LangGraph 官方 Orchestrator-Worker 模式实现：
- Planner 生成研究计划，interrupt 等待用户确认
- assign_workers 条件边函数通过 Send API 动态创建并行 Worker
- Worker 通过 Annotated[list, add] reducer 自动合并结果到主状态
- Replanner 评估结果，决定继续或生成报告
- Reporter 生成最终研究报告

Worker 类型（仅两种，职责清晰）：
- Search Worker：调用 SearchAgent 搜索站内文章/外部网页
- Knowledge Worker：调用 KnowledgeAgent 查询知识库
- 整合分析由 Reporter 统一完成（避免与 Replanner 职责重合）

架构（参考官方文档 https://docs.langchain.com/oss/python/langgraph/workflows-agents#orchestrator-worker）：
  planner → [interrupt] → assign_workers(Send API) → [workers 并行] → replanner
                            ↑                              │
                            └──── 继续执行 ─────────────────┘
                                                        ↓
                                                   reporter → END

文件：project-ai-agent/agents/research_agent.py
"""

import time
from typing import Optional, AsyncGenerator, Dict, Any, List

from langchain_deepseek import ChatDeepSeek
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import StateGraph, END
from langgraph.types import StreamWriter, Send, interrupt, Command
from loguru import logger

from config.settings import get_settings
from config.prompts.research_prompts import (
    get_planner_system_prompt,
    get_replanner_system_prompt,
    get_reporter_system_prompt,
)
from models.research_models import (
    ResearchTask,
    ResearchPlan,
    ResearchReport,
    ResearchAgentState,
    WorkerState,
    ReplannerOutput,
    ReporterOutput,
)


# ==================== 硬性终止条件 ====================

MAX_TASKS = 16          # 任务总数上限
MAX_ROUNDS = 3         # Replanner 迭代轮次上限
MAX_DURATION = 900     # 全局执行时间上限（秒，15 分钟）


class ResearchAgent:
    """
    ResearchAgent — LangGraph Orchestrator-Worker 模式

    核心设计（遵循官方模式）：
    1. assign_workers 是条件边函数（非节点），返回 Send 列表
    2. Worker 使用独立的 WorkerState，只接收单个任务
    3. Worker 通过 completed_tasks: Annotated[list, add] reducer 自动合并结果
    """

    def __init__(self):
        settings = get_settings()

        # ---- 思考模式配置 ----
        thinking_enabled = {
                "thinking": {"type": "enabled"},
                "reasoning_effort": "high",
            }

        # ---- Planner LLM：低温（0.1），开启思考模式 ----
        # 使用 json_mode 避免 tool_choice 与思考模式冲突
        self.llm_planner = ChatDeepSeek(
            model=settings.model_name_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.research_planner_temperature,
            streaming=True,
            extra_body=thinking_enabled,
        )

        # ---- Replanner LLM：思考模式开启 ----
        self.llm_replanner = ChatDeepSeek(
            model=settings.model_name_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.research_replanner_temperature,
            streaming=True,
            extra_body=thinking_enabled,
        )

        # ---- Reporter LLM：中温（0.4），开启思考 ----
        self.llm_reporter = ChatDeepSeek(
            model=settings.model_name_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.research_reporter_temperature,
            streaming=True,
            extra_body=thinking_enabled,
        )

        self.checkpointer = MemorySaver()
        self._start_time: Optional[float] = None
        self.graph = self._build_graph()

    def _build_graph(self):
        """
        构建 Orchestrator-Worker 工作流

        关键改进：将 planner 拆分为两个节点
        - plan_generator：生成计划 + 发射事件给前端（不含 interrupt，恢复时不重新执行）
        - plan_await_user：调用 interrupt() 等待用户，处理响应（恢复时仅重新执行此节点）
        遵循 LangGraph 官方原则："side effects before interrupt must be idempotent"，
        将非幂等的事件发射放入不含 interrupt 的节点中。
        """
        workflow = StateGraph(ResearchAgentState)  # type: ignore[arg-type]

        # ---- 注册节点 ----
        workflow.add_node("plan_generator", self._plan_generator_node)
        workflow.add_node("plan_await_user", self._plan_await_user_node)
        workflow.add_node("search_worker", self._search_worker_node)
        workflow.add_node("knowledge_worker", self._knowledge_worker_node)
        workflow.add_node("replanner", self._replanner_node)
        workflow.add_node("reporter", self._reporter_node)

        # ---- 入口 → plan_generator → plan_await_user ----
        workflow.set_entry_point("plan_generator")
        workflow.add_edge("plan_generator", "plan_await_user")

        # ---- plan_await_user 后路由 ----
        workflow.add_conditional_edges(
            "plan_await_user",
            self._route_after_plan_await_user,
            ["plan_generator", "search_worker", "knowledge_worker", END],
        )

        # ---- 所有 Worker 完成后进入 Replanner ----
        workflow.add_edge("search_worker", "replanner")
        workflow.add_edge("knowledge_worker", "replanner")

        # ---- Replanner 后路由：条件边函数返回 Send 列表或路由到 Reporter ----
        workflow.add_conditional_edges(
            "replanner",
            self._route_after_replanner,
            ["search_worker", "knowledge_worker", "reporter"],
        )

        # ---- Reporter 完成 → END ----
        workflow.add_edge("reporter", END)

        return workflow.compile(checkpointer=self.checkpointer)

    # ==================== 条件边函数（返回 Send 列表）====================

    def _route_after_plan_await_user(self, state: ResearchAgentState) -> List:
        """
        plan_await_user 后路由：条件边函数

        处理三种情况：
        1. needs_replan → 路由到 plan_generator（用户拒绝计划，需要重新生成）
        2. plan_approved → 返回 Send 列表，为每个 pending 任务创建 Worker
        3. 其他（interrupt 暂停）→ 路由到 END
        """
        current_step = state.get("current_step")

        # 用户拒绝计划，需要重新生成
        if current_step == "needs_replan":
            logger.info("[Route] 用户拒绝计划，重新路由到 plan_generator")
            return ["plan_generator"]

        # 计划已批准，开始执行任务
        if current_step == "plan_approved":
            return self._create_worker_sends(state)

        # interrupt 已在 plan_await_user 节点内暂停，此处路由到 END 不会实际执行
        return [END]

    def _route_after_replanner(self, state: ResearchAgentState) -> List:
        """
        Replanner 后路由：条件边函数

        - ready_to_report → 路由到 reporter
        - executing → 返回 Send 列表，为补充任务创建 Worker
        """
        if state.get("current_step") == "ready_to_report":
            return ["reporter"]

        # 有新的 pending 任务需要执行
        pending_tasks = [t for t in state.get("tasks", []) if t.status == "pending"]
        if pending_tasks:
            return self._create_worker_sends(state)

        # 没有待执行任务，进入报告生成
        return ["reporter"]

    def _create_worker_sends(self, state: ResearchAgentState) -> List[Send]:
        """
        为所有 pending 任务创建 Send 对象

        参考官方模式：
            return [Send("worker", {"section": s}) for s in state["sections"]]

        关键：Send 的第二个参数是 WorkerState 的字段，不是整个 state。
        """
        tasks = state.get("tasks", [])
        sends = []

        for task in tasks:
            if task.status == "pending":
                if task.agent_type == "search":
                    sends.append(Send("search_worker", {"task": task}))
                elif task.agent_type == "knowledge":
                    sends.append(Send("knowledge_worker", {"task": task}))
                else:
                    logger.warning(f"[Dispatch] 未知的 agent_type: {task.agent_type}")

        logger.info(f"[Dispatch] 创建 {len(sends)} 个 Worker Send")
        return sends

    # ==================== 节点实现 ====================

    # ==================== Planner 节点====================

    async def _plan_generator_node(self, state: ResearchAgentState, writer: StreamWriter) -> dict:
        """
        计划生成节点：分析用户需求，生成研究计划 + 发射事件给前端

        关键：本节点不含 interrupt()，因此恢复时不会重新执行。
        事件发射和计划生成只执行一次。
        当用户要求重新规划（needs_replan）时，依据反馈重新生成计划。
        """
        from services.core.llm_utils import streaming_structured_generate

        user_request = state["user_request"]
        clarified_requirements = state.get("clarified_requirements")
        user_feedback = state.get("user_feedback")

        # ---- 检查是否已有计划（从 needs_replan 恢复的情况）----
        existing_plan = state.get("plan")
        if existing_plan and state.get("current_step") == "needs_replan":
            # 用户拒绝后重新生成
            logger.info(f"[PlanGenerator] 用户拒绝计划，根据反馈重新生成: {user_feedback[:50] if user_feedback else '无反馈'}...")
            # 继续执行生成逻辑（不跳过）
        elif existing_plan:
            # 已有计划且不是需要重新规划，跳过生成
            logger.info(f"[PlanGenerator] 检测到已有计划，跳过生成: topic={existing_plan.topic}")
            return {}

        # ---- 生成新计划 ----
        logger.info(f"[PlanGenerator] 开始分析研究需求: {user_request[:50]}...")
        writer({"type": "phase", "data": {"phase": "planning", "step": "analyzing"}})

        prompt = get_planner_system_prompt(
            user_request=user_request,
            clarified_requirements=clarified_requirements,
            user_feedback=user_feedback,
        )

        try:
            plan: ResearchPlan = await streaming_structured_generate(
                prompt=prompt,
                output_model=ResearchPlan,
                llm=self.llm_planner,
                writer=writer,
                event_type="planner_thinking",
            )

            logger.info(f"[PlanGenerator] 计划生成完成: topic={plan.topic}, 任务数={len(plan.tasks)}")

            # ---- 发射事件 + 设置下一步状态 ----
            if plan.needs_clarification:
                logger.info("[PlanGenerator] 需求不够清晰，发射澄清事件")
                writer({"type": "clarification", "data": {"questions": plan.questions}})
                next_step = "awaiting_clarification"
            else:
                logger.info("[PlanGenerator] 发射计划确认事件")
                writer({"type": "plan_approval", "data": plan.model_dump()})
                next_step = "awaiting_approval"

            return {
                "plan": plan,
                "user_feedback": None,
                "clarified_requirements": None,
                "current_step": next_step,
            }

        except Exception as e:
            logger.error(f"[PlanGenerator] 生成计划失败: {e}")
            return {
                "error": {"message": str(e), "step": "plan_generator"},
                "current_step": "error",
            }

    async def _plan_await_user_node(self, state: ResearchAgentState, writer: StreamWriter) -> dict:
        """
        计划等待用户节点：调用 interrupt() 等待用户响应

        关键：本节点仅包含 interrupt() 和响应处理。事件发射已在 plan_generator
        中完成（不会被重新执行），恢复时仅重新执行此节点的逻辑，不产生副作用。
        """
        plan = state.get("plan")
        current_step = state.get("current_step", "")

        if not plan:
            logger.error("[PlanAwaitUser] 状态中没有计划，这不应该发生")
            return {
                "error": {"message": "状态中没有计划", "step": "plan_await_user"},
                "current_step": "error",
            }

        # ---- 中断点 1：需求模糊 → interrupt 等待用户回答 ----
        if current_step == "awaiting_clarification":
            user_answer = interrupt({
                "type": "clarification",
                "questions": plan.questions,
            })

            logger.info(f"[PlanAwaitUser] 用户回答澄清问题: {str(user_answer)[:50]}...")
            return {
                "clarified_requirements": user_answer,
                "current_step": "needs_replan",
            }

        # ---- 中断点 2：计划确认 → interrupt 等待用户确认/拒绝 ----
        if current_step == "awaiting_approval":
            user_response = interrupt({
                "type": "plan_approval",
                "plan": plan.model_dump(),
            })

            # 用户确认 → 计划通过，将任务状态设为 pending
            if user_response is True or user_response == "approve":
                logger.info("[PlanAwaitUser] 用户确认计划，进入执行阶段")
                tasks = [t.model_copy(update={"status": "pending"}) for t in plan.tasks]
                return {
                    "tasks": tasks,
                    "current_step": "plan_approved",
                    "current_round": 0,
                }

            # 用户拒绝，携带修改意见重新生成计划
            logger.info(f"[PlanAwaitUser] 用户拒绝计划，修改意见: {str(user_response)[:50]}")
            return {
                "user_feedback": str(user_response),
                "current_step": "needs_replan",
            }

        # 不应到达这里
        logger.error(f"[PlanAwaitUser] 未知的 current_step: {current_step}")
        return {"current_step": "error", "error": {"message": f"未知状态: {current_step}", "step": "plan_await_user"}}

    # ==================== Worker 节点（接收 WorkerState）====================

    async def _search_worker_node(self, state: WorkerState, writer: StreamWriter) -> dict:
        """
        Search Worker：调用 SearchAgent 执行搜索任务

        接收 WorkerState（含 task），返回 completed_tasks 供 reducer 自动合并。
        实时发射任务进度事件：搜索/爬取数量、完成状态。
        """
        from agents.sub_agents.search_agent import SearchAgent

        task = state["task"]
        logger.info(f"[SearchWorker] 执行搜索任务: {task.id} - {task.description[:50]}...")

        # 发射任务开始事件
        writer({"type": "task_progress", "data": {
            "task_id": task.id, "status": "executing", "step": "search",
            "message": "正在搜索..."
        }})

        try:
            search_agent = SearchAgent()

            # 拦截 sub_agent_tool_call 事件，统计搜索/爬取次数
            search_count = 0
            scrape_count = 0

            def intercepted_writer(data):
                """拦截 Sub-Agent 事件，统计工具调用次数并转发"""
                nonlocal search_count, scrape_count
                if isinstance(data, dict) and data.get("type") == "sub_agent_tool_call":
                    tool_name = data.get("tool_name", "")
                    # 搜索类工具
                    if any(kw in tool_name for kw in ("search", "find")):
                        search_count += 1
                        writer({"type": "task_progress", "data": {
                            "task_id": task.id, "status": "executing",
                            "search_count": search_count, "scrape_count": scrape_count,
                            "message": f"已搜索 {search_count} 次"
                        }})
                    # 爬取类工具
                    elif any(kw in tool_name for kw in ("scrape", "crawl")):
                        scrape_count += 1
                        writer({"type": "task_progress", "data": {
                            "task_id": task.id, "status": "executing",
                            "search_count": search_count, "scrape_count": scrape_count,
                            "message": f"已爬取 {scrape_count} 个网页"
                        }})
                # 转发原始事件
                writer(data)

            result = await search_agent.execute(task.description, stream_writer=intercepted_writer)

            # 生成简要摘要（截取前 100 字符，避免冗长）
            summary = f"搜索 {search_count} 次，爬取 {scrape_count} 个网页"

            logger.info(f"[SearchWorker] 搜索完成: {task.id}, {summary}")

            # 发射任务完成事件，包含简要摘要
            writer({"type": "task_progress", "data": {
                "task_id": task.id, "status": "completed",
                "search_count": search_count, "scrape_count": scrape_count,
                "summary": summary
            }})

            # 通过 completed_tasks reducer 写回结果，自动合并到主状态
            return {"completed_tasks": [{"task_id": task.id, "status": "completed", "result": result}]}
        except Exception as e:
            logger.error(f"[SearchWorker] 搜索失败: {e}")
            writer({"type": "task_progress", "data": {
                "task_id": task.id, "status": "failed", "summary": f"执行失败: {str(e)[:50]}"
            }})
            return {"completed_tasks": [{"task_id": task.id, "status": "failed", "result": str(e)}]}

    async def _knowledge_worker_node(self, state: WorkerState, writer: StreamWriter) -> dict:
        """
        Knowledge Worker：调用 KnowledgeAgent 执行知识库查询

        接收 WorkerState，通过 completed_tasks reducer 写回结果。
        实时发射任务进度事件：查询次数、完成状态。
        """
        from agents.sub_agents.knowledge_agent import KnowledgeAgent

        task = state["task"]
        logger.info(f"[KnowledgeWorker] 执行知识库查询: {task.id} - {task.description[:50]}...")

        # 发射任务开始事件
        writer({"type": "task_progress", "data": {
            "task_id": task.id, "status": "executing", "step": "knowledge",
            "message": "正在查询知识库..."
        }})

        try:
            knowledge_agent = KnowledgeAgent()

            # 拦截 sub_agent_tool_call 事件，统计查询次数
            query_count = 0

            def intercepted_writer(data):
                """拦截 Sub-Agent 事件，统计工具调用次数并转发"""
                nonlocal query_count
                if isinstance(data, dict) and data.get("type") == "sub_agent_tool_call":
                    tool_name = data.get("tool_name", "")
                    if "search" in tool_name or "knowledge" in tool_name:
                        query_count += 1
                        writer({"type": "task_progress", "data": {
                            "task_id": task.id, "status": "executing",
                            "query_count": query_count,
                            "message": f"已查询 {query_count} 次"
                        }})
                # 转发原始事件
                writer(data)

            result = await knowledge_agent.execute(task.description, stream_writer=intercepted_writer)

            # 生成简要摘要
            summary = f"查询知识库 {query_count} 次"

            logger.info(f"[KnowledgeWorker] 知识库查询完成: {task.id}, {summary}")

            # 发射任务完成事件，包含简要摘要
            writer({"type": "task_progress", "data": {
                "task_id": task.id, "status": "completed",
                "query_count": query_count,
                "summary": summary
            }})

            return {"completed_tasks": [{"task_id": task.id, "status": "completed", "result": result}]}
        except Exception as e:
            logger.error(f"[KnowledgeWorker] 知识库查询失败: {e}")
            writer({"type": "task_progress", "data": {
                "task_id": task.id, "status": "failed", "summary": f"执行失败: {str(e)[:50]}"
            }})
            return {"completed_tasks": [{"task_id": task.id, "status": "failed", "result": str(e)}]}

    # ==================== 决策节点 ====================

    async def _replanner_node(self, state: ResearchAgentState, writer: StreamWriter) -> dict:
        """
        Replanner 节点：评估执行结果，决定下一步

        从 completed_tasks（reducer 自动合并的 Worker 结果）中读取任务完成情况，
        更新 tasks 列表中对应任务的状态和结果，然后评估是否需要继续。
        """
        from services.core.llm_utils import structured_generate

        # ---- 从 completed_tasks 中合并结果到 tasks 列表 ----
        tasks = state.get("tasks", [])
        completed_task_results = state.get("completed_tasks", [])
        current_round = state.get("current_round", 0)

        tasks = self._merge_completed_results(tasks, completed_task_results)

        completed_tasks = [t for t in tasks if t.status == "completed"]
        completed_count = len(completed_tasks)
        total_count = len(tasks)

        logger.info(f"[Replanner] 评估执行结果: 完成={completed_count}/{total_count}, 轮次={current_round}")
        writer({"type": "phase", "data": {"phase": "evaluating", "step": "replanner"}})

        # ---- 硬性终止条件检查 ----
        elapsed_time = time.time() - (self._start_time or time.time())
        if total_count >= MAX_TASKS:
            logger.info(f"[Replanner] 达到任务上限 ({MAX_TASKS})，进入报告生成")
            return {"tasks": tasks, "current_step": "ready_to_report"}
        if current_round >= MAX_ROUNDS:
            logger.info(f"[Replanner] 达到轮次上限 ({MAX_ROUNDS})，进入报告生成")
            return {"tasks": tasks, "current_step": "ready_to_report"}
        if elapsed_time > MAX_DURATION:
            logger.info(f"[Replanner] 达到时间上限 ({MAX_DURATION}s)，进入报告生成")
            return {"tasks": tasks, "current_step": "ready_to_report"}

        # ---- 构建评估上下文 ----
        task_results_summary = self._build_task_results_summary(completed_tasks)
        findings_list = "\n".join(f"- {f}" for f in state.get("findings", []))

        plan = state.get("plan")
        prompt = get_replanner_system_prompt(
            topic=plan.topic if plan else state["user_request"],
            completed_count=completed_count,
            total_count=total_count,
            task_results_summary=task_results_summary,
            findings_list=findings_list,
            current_round=current_round,
        )

        try:
            output: ReplannerOutput = await structured_generate(
                prompt=prompt,
                output_model=ReplannerOutput,
                llm=self.llm_replanner,
                method="json_mode",
            )

            logger.info(f"[Replanner] 决策: {output.decision}, 洞察: {output.stage_insight[:50]}...")

            writer({
                "type": "stage_insight",
                "data": {
                    "round": current_round + 1,
                    "insight": output.stage_insight,
                    "completed_tasks": completed_count,
                    "total_tasks": total_count,
                },
            })

            # ---- 决策：完成 → 进入 Reporter ----
            if output.decision == "complete":
                return {"tasks": tasks, "current_step": "ready_to_report"}

            # ---- 决策：继续 → 生成补充任务 ----
            new_tasks = []
            for st in output.supplementary_tasks:
                new_task = ResearchTask(
                    id=f"t{len(tasks) + len(new_tasks) + 1}",
                    description=st.description,
                    agent_type=st.agent_type,
                    status="pending",
                )
                new_tasks.append(new_task)

            if new_tasks:
                writer({
                    "type": "replan",
                    "data": {
                        "round": current_round + 1,
                        "reason": output.replan_reason,
                        "added_tasks": [t.model_dump() for t in new_tasks],
                        "updated_task_list": [t.model_dump() for t in tasks + new_tasks],
                    },
                })

            return {
                "tasks": tasks + new_tasks,
                "current_step": "executing",
                "current_round": current_round + 1,
            }

        except Exception as e:
            logger.error(f"[Replanner] 评估失败: {e}")
            return {"tasks": tasks, "current_step": "ready_to_report"}

    async def _reporter_node(self, state: ResearchAgentState, writer: StreamWriter) -> dict:
        """
        Reporter 节点：整合所有发现，生成研究报告

        Reporter 拥有全局视角（所有任务结果 + 关键发现），负责最终的整合分析和报告生成。
        """
        from services.core.llm_utils import structured_generate

        tasks = self._merge_completed_results(
            state.get("tasks", []),
            state.get("completed_tasks", []),
        )
        plan = state.get("plan")
        topic = plan.topic if plan else state["user_request"]

        logger.info(f"[Reporter] 开始生成研究报告: {topic}")
        writer({"type": "phase", "data": {"phase": "reporting", "step": "generating"}})

        task_results = self._build_task_results_summary(
            [t for t in tasks if t.status == "completed"]
        )
        findings_list = "\n".join(f"- {f}" for f in state.get("findings", []))

        prompt = get_reporter_system_prompt(
            topic=topic,
            task_results=task_results,
            findings_list=findings_list,
        )

        try:
            output: ReporterOutput = await structured_generate(
                prompt=prompt,
                output_model=ReporterOutput,
                llm=self.llm_reporter,
                method="json_mode",
            )

            report = ResearchReport(
                topic=topic,
                content=output.content,
                summary=output.summary,
                key_findings=output.key_findings,
                sources=output.sources,
            )

            logger.info(f"[Reporter] 报告生成完成，长度: {len(output.content)} 字符")

            task_id = state["task_id"]
            await self._persist_report(task_id, report)

            writer({
                "type": "report_ready",
                "data": {
                    "topic": topic,
                    "summary": report.summary,
                    "key_findings": report.key_findings,
                    "content": report.content,
                },
            })

            return {"report": report, "current_step": "completed"}

        except Exception as e:
            logger.error(f"[Reporter] 生成报告失败: {e}")
            return {"error": {"message": str(e), "step": "reporter"}, "current_step": "error"}

    # ==================== 辅助方法 ====================

    def _merge_completed_results(
        self,
        tasks: List[ResearchTask],
        completed_task_results: List[dict],
    ) -> List[ResearchTask]:
        """
        将 Worker 通过 reducer 合并的 completed_tasks 结果更新到 tasks 列表中

        completed_tasks 格式：[{"task_id": "t1", "status": "completed", "result": "..."}, ...]
        """
        if not completed_task_results:
            return tasks

        result_map = {r["task_id"]: r for r in completed_task_results}

        updated_tasks = []
        for task in tasks:
            if task.id in result_map:
                cr = result_map[task.id]
                updated_tasks.append(task.model_copy(update={
                    "status": cr["status"],
                    "result": cr.get("result"),
                }))
            else:
                updated_tasks.append(task)

        return updated_tasks

    def _build_task_results_summary(self, tasks: List[ResearchTask]) -> str:
        """构建任务结果摘要文本"""
        if not tasks:
            return "暂无已完成的任务"

        parts = []
        for task in tasks:
            result_text = task.result[:500] if task.result else "无结果"
            parts.append(f"任务 [{task.id}] {task.description}:\n{result_text}")

        return "\n\n---\n\n".join(parts)

    async def _persist_report(self, task_id: str, report: ResearchReport):
        """回调 Java 内部接口持久化研究报告"""
        from services.business.research_service import get_research_service

        research_service = get_research_service()
        report_url = await research_service.persist_report(
            task_id=task_id,
            content=report.content,
            summary=report.summary,
            key_findings=report.key_findings,
            sources=report.sources,
        )
        if report_url:
            report.report_url = report_url

    # ==================== 对外接口 ====================

    async def start_research(
        self,
        user_request: str,
        thread_id: str,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        启动深度研究任务（流式返回 SSE 事件）

        Args:
            user_request: 用户研究需求
            thread_id: 线程 ID（通常传入 task_id）
        """
        config = {"configurable": {"thread_id": thread_id}}
        initial_state = {"task_id": thread_id, "user_request": user_request}

        self._start_time = time.time()
        logger.info(f"[Research] 启动研究任务, thread_id={thread_id}, 需求={user_request[:50]}...")

        async for mode, event in self.graph.astream(
            initial_state,
            config,
            stream_mode=["custom", "updates"],
        ):
            if mode == "custom":
                yield event
            elif mode == "updates":
                for node_name, node_output in event.items():
                    if node_name in ("search_worker", "knowledge_worker"):
                        yield {"type": "phase", "data": {"phase": "worker_completed", "node": node_name}}
                    elif node_name == "reporter":
                        yield {"type": "phase", "data": {"phase": "completed", "step": "done"}}

        yield {"type": "done", "data": None}

    async def resume_research(
        self,
        thread_id: str,
        response: Any,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        恢复被中断的研究任务

        Args:
            thread_id: 线程 ID（与 start_research 传入的一致）
            response: 用户响应
        """
        config = {"configurable": {"thread_id": thread_id}}

        logger.info(f"[Research] 恢复研究任务, thread_id={thread_id}")

        async for mode, event in self.graph.astream(
            Command(resume=response),
            config,
            stream_mode=["custom", "updates"],
        ):
            if mode == "custom":
                yield event
            elif mode == "updates":
                for node_name, node_output in event.items():
                    if node_name in ("search_worker", "knowledge_worker"):
                        yield {"type": "phase", "data": {"phase": "worker_completed", "node": node_name}}
                    elif node_name == "reporter":
                        yield {"type": "phase", "data": {"phase": "completed", "step": "done"}}

        yield {"type": "done", "data": None}


# ==================== 单例 ====================

_research_agent: Optional[ResearchAgent] = None


def get_research_agent() -> ResearchAgent:
    """获取 ResearchAgent 单例"""
    global _research_agent
    if _research_agent is None:
        _research_agent = ResearchAgent()
    return _research_agent