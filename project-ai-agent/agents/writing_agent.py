"""
智能写作 Agent（LangGraph 工作流）
文件：project-ai-agent/agents/writing_agent.py
"""

import asyncio
import json
from typing import Optional, AsyncGenerator, Dict, Any, Callable

from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import MemorySaver
from langgraph.checkpoint.serde.jsonplus import JsonPlusSerializer
from langgraph.graph import StateGraph, END
from loguru import logger

from config.prompts import get_prompt_manager
from config.settings import get_settings
from models.writing_models import WritingPlan, WritingResult, WritingAgentState
from services.business.writing.writing_content_service import WritingContentService
from services.business.writing.writing_quality_service import WritingQualityService
from services.business.writing.writing_tag_service import WritingTagService
from services.core.llm_utils import structured_generate


class WritingAgent:
    """AI 智能写作 Agent（Plan-and-Execute 模式）"""

    def __init__(self):
        settings = get_settings()
        self.prompt_manager = get_prompt_manager()

        # 四角色 LLM 配置：不同节点使用不同的 temperature，提升各环节输出质量
        # 规划角色：低温（0.1）保证写作计划精确、结构清晰
        self.llm_planner = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.writing_planner_temperature,
            streaming=True
        )
        # 写作角色：中高温（0.6）提升内容创造力、语言生动性
        self.llm_writer = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.writing_writer_temperature,
            streaming=True
        )
        # 评估角色：低温（0.1）确保评分标准一致、客观公正
        self.llm_critic = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.writing_critic_temperature,
            streaming=True
        )
        # 分类角色：较低温（0.2）保证分类结果确定、可复现
        self.llm_classifier = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.writing_classifier_temperature,
            streaming=True
        )

        # 注入独立的业务 Service，职责清晰、可独立测试
        self.content_service = WritingContentService(self.llm_writer)
        self.quality_service = WritingQualityService(self.llm_critic)
        self.tag_service = WritingTagService(self.llm_classifier)

        self.checkpointer = MemorySaver(serde=JsonPlusSerializer())
        self.max_revisions = settings.writing_max_revisions
        self.reflection_threshold = settings.writing_reflection_threshold
        self.progress_callback: Optional[Callable[[Dict[str, Any]], None]] = None

        self.graph = self._build_graph()

    def _build_graph(self):
        """构建 Plan-and-Execute 工作流"""
        workflow = StateGraph(WritingAgentState) # type: ignore[arg-type]

        workflow.add_node("plan", self._plan_node) # type: ignore[arg-type]
        workflow.add_node("execute", self._execute_node) # type: ignore[arg-type]
        workflow.add_node("reflect", self._reflect_node) # type: ignore[arg-type]
        workflow.add_node("finalize", self._finalize_node) # type: ignore[arg-type]

        workflow.set_entry_point("plan")

        workflow.add_edge("plan", "execute")
        workflow.add_edge("execute", "reflect")

        workflow.add_conditional_edges(
            "reflect",
            self._route_after_reflection,
            {
                "retry": "execute",
                "proceed": "finalize"
            }
        )

        workflow.add_conditional_edges(
            "finalize",
            self._route_after_finalize,
            {
                "complete": END,
                "revise_plan": "plan"
            }
        )

        return workflow.compile(checkpointer=self.checkpointer, interrupt_before=["execute"])

    async def _plan_node(self, state: WritingAgentState) -> dict:
        """
        Plan 节点：分析写作需求，生成写作计划

        """
        user_request = state["user_request"]
        plan_feedback = state.get("plan_feedback")

        logger.info(f"[Plan] 开始生成写作计划，用户需求: {user_request[:50]}...")

        if plan_feedback:
            prompt = self.prompt_manager.get_writing_revision_prompt(user_request, state.get("plan"), plan_feedback)
        else:
            prompt = self.prompt_manager.get_writing_plan_prompt(user_request)

        try:
            plan: WritingPlan = await structured_generate(prompt, WritingPlan, self.llm_planner)

            references = []
            if plan.reference_keywords:
                references = await self._search_references(plan.reference_keywords, plan.topic)

            logger.info(f"[Plan] 写作计划生成完成，主题: {plan.topic}")

            return {
                "plan": plan,
                "references": references,
                "current_step": "plan_generated"
            }
        except Exception as e:
            logger.error(f"[Plan] 生成计划失败: {e}")
            return {
                "error": {"message": str(e), "step": "plan"},
                "current_step": "error"
            }

    async def _execute_node(self, state: WritingAgentState) -> dict:
        """
        Execute 节点：并行执行标题+标签→摘要→正文

        并行优化（P2）：
          title 和 tags 都只依赖 plan，互不依赖 → 通过 asyncio.gather 并行执行。
          summary 依赖 title，content 依赖 title + summary → 保持串行。

        依赖关系：
          Phase 1（并行）: title + tags          ← 只依赖 plan
          Phase 2（串行）: summary                ← 依赖 title
          Phase 3（串行）: content                ← 依赖 title + summary + references

        每个子步骤通过 self.progress_callback 实时推送进度事件，
        确保前端 SSE 流可以实时获取当前写作阶段。

        当从 reflect 节点重试时（current_step == "revised"），保留已修订的内容，
        不重新生成，因为 reflect 节点已调用 quality_service.revise 完成了内容修订。
        """
        plan = state["plan"]
        references = state.get("references", [])
        current_step = state.get("current_step")

        logger.info(f"[Execute] 开始执行写作任务，主题: {plan.topic}")

        # 当从 reflect 修订后重试时，跳过重新生成，保留已修订的结果
        if current_step == "revised":
            logger.info("[Execute] 保留修订后的内容，跳过重新生成")
            return {
                "writing_result": state.get("writing_result"),
                "current_step": "executed"
            }

        result = WritingResult(
            title="", summary="", content=""
        )

        # ==================== Phase 1：title + tags 并行 ====================
        try:
            if self.progress_callback:
                await self.progress_callback({
                    "type": "phase",
                    "data": {"phase": "executing", "step": "title"}
                })
            if self.progress_callback:
                await self.progress_callback({
                    "type": "phase",
                    "data": {"phase": "executing", "step": "tags"}
                })

            logger.info("[Execute] 并行生成标题和标签分类...")
            title_val, tag_result = await asyncio.gather(
                self.content_service.generate_title(plan),
                self.tag_service.generate_tags(plan),
            )

            result.title = title_val
            if self.progress_callback:
                await self.progress_callback({
                    "type": "token",
                    "data": result.title
                })

            result.category_name = tag_result.get("category_name")
            result.category_id = tag_result.get("category_id")
            result.tag_names = tag_result.get("tag_names", [])
            result.tag_ids = tag_result.get("tag_ids", [])
            if self.progress_callback:
                await self.progress_callback({
                    "type": "token",
                    "data": json.dumps({
                        "category_name": result.category_name,
                        "category_id": result.category_id,
                        "tag_names": result.tag_names,
                        "tag_ids": result.tag_ids
                    }, ensure_ascii=False)
                })

        except Exception as e:
            logger.error(f"[Execute] 并行阶段(title/tags)失败: {e}")
            return {
                "error": {"message": str(e), "step": "title_or_tags"},
                "current_step": "error"
            }

        # ==================== Phase 2：summary（依赖 title）= ====================
        try:
            if self.progress_callback:
                await self.progress_callback({
                    "type": "phase",
                    "data": {"phase": "executing", "step": "summary"}
                })

            logger.info("[Execute] 生成摘要...")
            result.summary = await self.content_service.generate_summary(plan, result.title)
            if self.progress_callback:
                await self.progress_callback({
                    "type": "token",
                    "data": result.summary
                })

        except Exception as e:
            logger.error(f"[Execute] summary 步骤失败: {e}")
            return {
                "error": {"message": str(e), "step": "summary"},
                "current_step": "error"
            }

        # ==================== Phase 3：content（依赖 title + summary）= ====================
        try:
            if self.progress_callback:
                await self.progress_callback({
                    "type": "phase",
                    "data": {"phase": "executing", "step": "content"}
                })

            logger.info("[Execute] 生成正文...")
            result.content = await self.content_service.generate_content(plan, result.title, result.summary, references)
            if self.progress_callback:
                await self.progress_callback({
                    "type": "token",
                    "data": result.content
                })

        except Exception as e:
            logger.error(f"[Execute] content 步骤失败: {e}")
            return {
                "error": {"message": str(e), "step": "content"},
                "current_step": "error"
            }

        logger.info("[Execute] 写作任务执行完成")

        return {
            "writing_result": result,
            "current_step": "executed"
        }

    async def _reflect_node(self, state: WritingAgentState) -> dict:
        """
        Reflection 节点：评估写作质量，必要时自动微调
        """
        plan = state["plan"]
        result = state["writing_result"]
        revision_count = state.get("revision_count", 0)

        logger.info(f"[Reflect] 开始评估写作质量，当前修订次数: {revision_count}")

        try:
            reflection = await self.quality_service.evaluate(plan, result)

            logger.info(f"[Reflect] 评估完成，综合评分: {reflection.score}")

            if reflection.score < self.reflection_threshold and revision_count < self.max_revisions:
                logger.info(f"[Reflect] 评分低于阈值({self.reflection_threshold})，准备微调...")

                revised_result = await self.quality_service.revise(result, reflection)

                return {
                    "reflection": reflection,
                    "writing_result": revised_result,
                    "revision_count": revision_count + 1,
                    "current_step": "revised"
                }

            return {
                "reflection": reflection,
                "current_step": "reflected"
            }
        except Exception as e:
            logger.error(f"[Reflect] 评估失败: {e}")
            return {
                "error": {"message": str(e), "step": "reflect"},
                "current_step": "error"
            }

    async def _finalize_node(self, state: WritingAgentState) -> dict:
        """
        Finalize 节点：整理写作结果，标记任务完成

        不调用任何保存接口（文章保存由前端直调 Java 后端）
        """
        result = state.get("writing_result")

        logger.info("[Finalize] 整理写作结果，标记任务完成")

        return {
            "current_step": "finalized",
            "writing_result": result
        }

    def _route_after_reflection(self, state: WritingAgentState) -> str:
        """
        反思后路由逻辑

        Returns:
            "retry": 返回 execute 节点重新执行
            "proceed": 进入 finalize 节点
        """
        reflection = state.get("reflection")
        revision_count = state.get("revision_count", 0)

        if reflection and reflection.score < self.reflection_threshold and revision_count < self.max_revisions:
            logger.info(f"[Route] 评分 {reflection.score} < {self.reflection_threshold}，返回 execute 重试")
            return "retry"

        logger.info(f"[Route] 评分达标或已达最大修订次数，进入 finalize")
        return "proceed"

    def _route_after_finalize(self, state: WritingAgentState) -> str:
        """
        终态确认后路由

        Returns:
            "complete": 结束工作流
            "revise_plan": 返回 plan 节点重新规划
        """
        action = state.get("action", "complete")

        if action == "revise_plan":
            logger.info("[Route] 用户要求重新规划，返回 plan 节点")
            return "revise_plan"

        logger.info("[Route] 任务完成，结束工作流")
        return "complete"

    async def _search_references(self, keywords: list, topic: str = "") -> list:
        """
        智能搜索参考资料

        搜索策略：
        1. 先搜索站内文章（使用ES）
        2. 如果站内结果不足（<3条），再搜索外部技术博客
        3. 合并结果并返回

        Args:
            keywords: 搜索关键词列表
            topic: 文章主题（用于外部搜索补充）

        Returns:
            参考资料列表
        """
        try:
            from tools.smart_search_tool import smart_search_references

            logger.info(f"[Search] 启动智能搜索，关键词: {keywords[:3]}")
            results = await smart_search_references.ainvoke({
                "keywords": keywords,
                "topic": topic
            })

            if isinstance(results, list):
                references = results
                logger.info(f"[Search] 智能搜索完成，获取 {len(references)} 条参考资料")
            else:
                logger.warning(f"[Search] 智能搜索返回格式异常，回退到基础搜索")
                references = await self._fallback_search(keywords)

        except Exception as e:
            logger.warning(f"[Search] 智能搜索失败，回退到基础搜索: {e}")
            references = await self._fallback_search(keywords)

        return references

    async def _fallback_search(self, keywords: list) -> list:
        """
        基础搜索（回退方案）

        Args:
            keywords: 搜索关键词列表

        Returns:
            参考资料列表
        """
        references = []

        try:
            from tools.article_tool import search_articles_by_keyword

            for keyword in keywords[:3]:
                try:
                    results = await search_articles_by_keyword.ainvoke({"keyword": keyword})
                    if isinstance(results, list) and len(results) > 0:
                        references.extend(results[:2])
                except Exception as e:
                    logger.warning(f"[Search] 基础搜索失败 '{keyword}': {e}")

            logger.info(f"[Search] 基础搜索完成，获取 {len(references)} 条结果")

        except Exception as e:
            logger.error(f"[Search] 基础搜索完全失败: {e}")

        return references

    async def generate_plan(
        self,
        initial_state: WritingAgentState,
        thread_id: str = "default"
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        阶段1：仅生成写作计划，然后停止等待用户确认

        使用 LangGraph astream 流式执行，生成计划后自动停在
        interrupt_before=["execute"] 中断点，等待用户确认。
        生成的计划可从 graph 检查点恢复。

        Args:
            initial_state: 初始状态
            thread_id: 线程 ID（用于检查点持久化，通常传入 task_id）

        Yields:
            phase/plan_ready/error 事件
        """
        config = {"configurable": {"thread_id": thread_id}}

        yield {"type": "phase", "data": {"phase": "planning", "step": "generating_plan"}}

        async for event in self.graph.astream(initial_state, config):
            if "plan" in event:
                node_output = event["plan"]

                if node_output.get("error"):
                    yield {"type": "error", "data": node_output["error"]["message"]}
                    return

                yield {
                    "type": "plan_ready",
                    "data": node_output["plan"].model_dump()
                }
                break

    async def execute_stream(
        self,
        thread_id: str
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        阶段2：根据已确认的计划，执行写作流程

        从 graph 检查点恢复执行（graph 停在 interrupt_before=["execute"]），
        自动运行 execute → reflect → (retry loop) → finalize → END。

        execute 节点的子步骤事件（phase/token）由 progress_callback 实时推送，
        此方法只处理 reflect 和 finalize 节点的事件。

        Args:
            thread_id: 线程 ID（与 generate_plan 传入的 thread_id 一致）

        Yields:
            reflection_result/finalize_ready/done 事件
        """
        config = {"configurable": {"thread_id": thread_id}}

        async for event in self.graph.astream(None, config):
            if "reflect" in event:
                node_output = event["reflect"]
                reflection = node_output.get("reflection")
                if reflection:
                    yield {"type": "phase", "data": {"phase": "reflecting"}}
                    yield {
                        "type": "reflection_result",
                        "data": reflection.model_dump()
                    }

            elif "finalize" in event:
                node_output = event["finalize"]
                writing_result = node_output.get("writing_result")
                if writing_result:
                    yield {
                        "type": "finalize_ready",
                        "data": writing_result.model_dump(by_alias=True)
                    }

        yield {"type": "done", "data": None}

    async def arun(self, initial_state: WritingAgentState) -> dict:
        """
        异步运行写作 Agent（非流式）

        Args:
            initial_state: 初始状态

        Returns:
            最终状态
        """
        final_state = await self.graph.ainvoke(initial_state)

        return final_state


_writing_agent: Optional[WritingAgent] = None


def get_writing_agent() -> WritingAgent:
    """获取 Writing Agent 单例"""
    global _writing_agent
    if _writing_agent is None:
        _writing_agent = WritingAgent()
    return _writing_agent
