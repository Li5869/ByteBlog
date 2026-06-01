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

        # DeepSeek thinking模式不支持 tool_choice 参数，而 with_structured_output(method="function_calling")
        # 会通过 bind_tools 设置 tool_choice，导致 400 错误。
        # 解决方案：通过 extra_body 显式禁用 thinking 模式
        thinking_disabled = {"thinking": {"type": "disabled"}}

        # 四角色 LLM 配置：不同节点使用不同的 temperature，提升各环节输出质量
        # 规划角色：低温（0.1）保证写作计划精确、结构清晰
        self.llm_planner = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.writing_planner_temperature,
            streaming=True,
            extra_body=thinking_disabled
        )
        # 写作角色：中高温（0.6）提升内容创造力、语言生动性
        self.llm_writer = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.writing_writer_temperature,
            streaming=True,
            extra_body=thinking_disabled
        )
        # 评估角色：低温（0.1）确保评分标准一致、客观公正
        self.llm_critic = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.writing_critic_temperature,
            streaming=True,
            extra_body=thinking_disabled
        )
        # 分类角色：较低温（0.2）保证分类结果确定、可复现
        self.llm_classifier = ChatOpenAI(
            model=settings.model_name_deepseek,
            base_url=settings.openai_base_url_deepseek,
            api_key=settings.openai_api_key_deepseek,
            temperature=settings.writing_classifier_temperature,
            streaming=True,
            extra_body=thinking_disabled
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
        """
        构建 Plan-and-Execute 工作流（节点拆分版本）

        架构：
          plan ──┬── generate_title ──┐
                 │                    ├── merge → summary → content → evaluate → finalize
                 └── generate_tags ───┘                                          ↑
                                                            revise ──────────────┘
        """
        workflow = StateGraph(WritingAgentState) # type: ignore[arg-type]

        # 添加所有节点
        workflow.add_node("plan", self._plan_node) # type: ignore[arg-type]
        workflow.add_node("generate_title", self._generate_title_node) # type: ignore[arg-type]
        workflow.add_node("generate_tags", self._generate_tags_node) # type: ignore[arg-type]
        workflow.add_node("merge_title_tags", self._merge_title_tags_node) # type: ignore[arg-type]
        workflow.add_node("generate_summary", self._generate_summary_node) # type: ignore[arg-type]
        workflow.add_node("generate_content", self._generate_content_node) # type: ignore[arg-type]
        workflow.add_node("evaluate", self._evaluate_node) # type: ignore[arg-type]
        workflow.add_node("revise", self._revise_node) # type: ignore[arg-type]
        workflow.add_node("finalize", self._finalize_node) # type: ignore[arg-type]

        # 入口点
        workflow.set_entry_point("plan")

        # Phase 1: title 和 tags 并行启动（LangGraph 原生并行模式）
        workflow.add_edge("plan", "generate_title")
        workflow.add_edge("plan", "generate_tags")

        # 汇合点：等待两者都完成后合并结果
        workflow.add_edge("generate_title", "merge_title_tags")
        workflow.add_edge("generate_tags", "merge_title_tags")

        # Phase 2 & 3: 串行执行
        workflow.add_edge("merge_title_tags", "generate_summary")
        workflow.add_edge("generate_summary", "generate_content")
        workflow.add_edge("generate_content", "evaluate")

        # Evaluator-Optimizer 循环：评分低时进入 revise 节点
        workflow.add_conditional_edges(
            "evaluate",
            self._route_after_evaluation,
            {
                "revise": "revise",
                "proceed": "finalize"
            }
        )

        # 修订后重新评估
        workflow.add_edge("revise", "evaluate")

        # 终态路由
        workflow.add_conditional_edges(
            "finalize",
            self._route_after_finalize,
            {
                "complete": END,
                "revise_plan": "plan"
            }
        )

        # 中断点：在并行节点执行前等待用户确认
        return workflow.compile(checkpointer=self.checkpointer, interrupt_before=["generate_title", "generate_tags"])

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

    # ==================== 拆分后的独立节点 ====================

    async def _generate_title_node(self, state: WritingAgentState) -> dict:
        """
        标题生成节点：只负责生成标题

        与 generate_tags 并行执行，都只依赖 plan。
        输出写入 parallel_outputs 字段，由 merge 节点合并。
        使用 Annotated[list, add] reducer 自动合并并行节点输出。
        """
        plan = state["plan"]

        logger.info("[Title] 开始生成标题...")

        if self.progress_callback:
            await self.progress_callback({
                "type": "phase",
                "data": {"phase": "executing", "step": "title"}
            })

        try:
            title = await self.content_service.generate_title(plan)

            if self.progress_callback:
                await self.progress_callback({
                    "type": "token",
                    "data": title
                })

            logger.info(f"[Title] 标题生成完成: {title[:30]}...")

            return {
                "parallel_outputs": [{"type": "title", "value": title}]
            }
        except Exception as e:
            logger.error(f"[Title] 生成标题失败: {e}")
            return {
                "error": {"message": str(e), "step": "title"}
            }

    async def _generate_tags_node(self, state: WritingAgentState) -> dict:
        """
        标签分类节点：只负责生成标签和分类

        与 generate_title 并行执行，都只依赖 plan。
        输出写入 parallel_outputs 字段，由 merge 节点合并。
        使用 Annotated[list, add] reducer 自动合并并行节点输出。
        """
        plan = state["plan"]

        logger.info("[Tags] 开始生成标签分类...")

        if self.progress_callback:
            await self.progress_callback({
                "type": "phase",
                "data": {"phase": "executing", "step": "tags"}
            })

        try:
            tag_result = await self.tag_service.generate_tags(plan)

            if self.progress_callback:
                await self.progress_callback({
                    "type": "token",
                    "data": json.dumps({
                        "category_name": tag_result.get("category_name"),
                        "category_id": tag_result.get("category_id"),
                        "tag_names": tag_result.get("tag_names", []),
                        "tag_ids": tag_result.get("tag_ids", []),
                        "all_tag_names": tag_result.get("all_tag_names", [])
                    }, ensure_ascii=False)
                })

            logger.info("[Tags] 标签生成完成")

            return {
                "parallel_outputs": [{
                    "type": "tags",
                    "value": {
                        "category_name": tag_result.get("category_name"),
                        "category_id": tag_result.get("category_id"),
                        "tag_names": tag_result.get("tag_names", []),
                        "tag_ids": tag_result.get("tag_ids", []),
                        "all_tag_names": tag_result.get("all_tag_names", [])
                    }
                }]
            }
        except Exception as e:
            logger.error(f"[Tags] 生成标签失败: {e}")
            return {
                "error": {"message": str(e), "step": "tags"}
            }

    async def _merge_title_tags_node(self, state: WritingAgentState) -> dict:
        """
        合并节点：等待 title 和 tags 都完成后，合并到 writing_result

        LangGraph 并行节点通过 Annotated[list, add] reducer 自动合并输出到 parallel_outputs，
        此节点作为汇合点，从 parallel_outputs 中提取并合并为完整的 WritingResult。

        tag_names 原始值只包含新标签名称，需 Java 后端创建后关联。
        all_tag_names 包含所有标签名称（新标签 + 已存在标签）。
        """
        logger.info("[Merge] 合并标题和标签结果...")

        outputs = state.get("parallel_outputs", [])
        title_output = next((o for o in outputs if o["type"] == "title"), None)
        tags_output = next((o for o in outputs if o["type"] == "tags"), None)

        writing_result = WritingResult(
            title=title_output["value"] if title_output else "",
            summary="",
            content="",
            category_name=tags_output["value"].get("category_name") if tags_output else None,
            category_id=tags_output["value"].get("category_id") if tags_output else None,
            tag_names=tags_output["value"].get("tag_names", []) if tags_output else [],
            tag_ids=tags_output["value"].get("tag_ids", []) if tags_output else [],
            all_tag_names=tags_output["value"].get("all_tag_names", []) if tags_output else []
        )

        logger.info(f"[Merge] 合并完成，标题: {writing_result.title[:30] if writing_result.title else 'N/A'}...")

        return {
            "writing_result": writing_result,
            "parallel_outputs": [],  # 清空临时列表
            "current_step": "title_tags_merged"
        }

    async def _generate_summary_node(self, state: WritingAgentState) -> dict:
        """
        摘要生成节点：依赖 title（由 merge 节点合并后写入 writing_result）
        """
        plan = state["plan"]
        writing_result = state.get("writing_result")

        if not writing_result or not writing_result.title:
            logger.error("[Summary] 标题未生成，无法生成摘要")
            return {
                "error": {"message": "标题未生成", "step": "summary"},
                "current_step": "error"
            }

        logger.info("[Summary] 开始生成摘要...")

        if self.progress_callback:
            await self.progress_callback({
                "type": "phase",
                "data": {"phase": "executing", "step": "summary"}
            })

        try:
            summary = await self.content_service.generate_summary(plan, writing_result.title)

            if self.progress_callback:
                await self.progress_callback({
                    "type": "token",
                    "data": summary
                })

            logger.info(f"[Summary] 摘要生成完成: {summary[:50]}...")

            return {
                "writing_result": writing_result.model_copy(update={"summary": summary}),
                "current_step": "summary_generated"
            }
        except Exception as e:
            logger.error(f"[Summary] 生成摘要失败: {e}")
            return {
                "error": {"message": str(e), "step": "summary"},
                "current_step": "error"
            }

    async def _generate_content_node(self, state: WritingAgentState) -> dict:
        """
        正文生成节点：依赖 title + summary + references
        """
        plan = state["plan"]
        writing_result = state.get("writing_result")
        references = state.get("references", [])

        if not writing_result or not writing_result.title or not writing_result.summary:
            logger.error("[Content] 标题或摘要未生成，无法生成正文")
            return {
                "error": {"message": "标题或摘要未生成", "step": "content"},
                "current_step": "error"
            }

        logger.info("[Content] 开始生成正文...")

        if self.progress_callback:
            await self.progress_callback({
                "type": "phase",
                "data": {"phase": "executing", "step": "content"}
            })

        try:
            content = await self.content_service.generate_content(
                plan, writing_result.title, writing_result.summary, references
            )

            if self.progress_callback:
                await self.progress_callback({
                    "type": "token",
                    "data": content
                })

            logger.info(f"[Content] 正文生成完成，长度: {len(content)} 字符")

            return {
                "writing_result": writing_result.model_copy(update={"content": content}),
                "current_step": "executed"
            }
        except Exception as e:
            logger.error(f"[Content] 生成正文失败: {e}")
            return {
                "error": {"message": str(e), "step": "content"},
                "current_step": "error"
            }

    async def _evaluate_node(self, state: WritingAgentState) -> dict:
        """
        评估节点：只负责评估写作质量，不进行修订

        根据评分决定下一步：
        - 评分达标 → 进入 finalize
        - 评分不达标 → 进入 revise
        """
        plan = state["plan"]
        result = state["writing_result"]
        revision_count = state.get("revision_count", 0)

        logger.info(f"[Evaluate] 开始评估写作质量，当前修订次数: {revision_count}")

        # 评估开始前推送进度事件
        if self.progress_callback:
            await self.progress_callback({
                "type": "phase",
                "data": {"phase": "evaluating", "step": "evaluating"}
            })

        try:
            reflection = await self.quality_service.evaluate(plan, result)

            logger.info(f"[Evaluate] 评估完成，综合评分: {reflection.score}")

            return {
                "reflection": reflection,
                "revision_count": revision_count,
                "current_step": "evaluated"
            }
        except Exception as e:
            logger.error(f"[Evaluate] 评估失败: {e}")
            return {
                "error": {"message": str(e), "step": "evaluate"},
                "current_step": "error"
            }

    async def _revise_node(self, state: WritingAgentState) -> dict:
        """
        修订节点：根据评估结果修订文章内容

        只有在评分不达标且未超过最大修订次数时才会执行
        """
        result = state["writing_result"]
        reflection = state["reflection"]
        revision_count = state.get("revision_count", 0)

        logger.info(f"[Revise] 开始修订文章，当前修订次数: {revision_count}")

        # 修订开始前推送进度事件
        if self.progress_callback:
            await self.progress_callback({
                "type": "phase",
                "data": {"phase": "revising", "step": "revising"}
            })

        try:
            revised_result = await self.quality_service.revise(result, reflection)

            logger.info(f"[Revise] 修订完成，修订次数: {revision_count + 1}")

            return {
                "writing_result": revised_result,
                "revision_count": revision_count + 1,
                "current_step": "revised"
            }
        except Exception as e:
            logger.error(f"[Revise] 修订失败: {e}")
            return {
                "error": {"message": str(e), "step": "revise"},
                "current_step": "error"
            }

    def _route_after_evaluation(self, state: WritingAgentState) -> str:
        """
        评估后路由逻辑

        Returns:
            "revise": 进入 revise 节点修订
            "proceed": 进入 finalize 节点
        """
        reflection = state.get("reflection")
        revision_count = state.get("revision_count", 0)

        if reflection and reflection.score < self.reflection_threshold and revision_count < self.max_revisions:
            logger.info(f"[Route] 评分 {reflection.score} < {self.reflection_threshold}，进入修订")
            return "revise"

        logger.info(f"[Route] 评分达标或已达最大修订次数，进入 finalize")
        return "proceed"

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

        yield {"type": "phase", "data": {"phase": "planning", "step": "planning"}}

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

        从 graph 检查点恢复执行（graph 停在 interrupt_before），
        自动运行 generate_title/generate_tags → merge → summary → content → evaluate → revise → finalize。

        各节点的子步骤事件（phase/token）由 progress_callback 实时推送，
        此方法只处理 evaluate、revise 和 finalize 节点的事件。

        Args:
            thread_id: 线程 ID（与 generate_plan 传入的 thread_id 一致）

        Yields:
            reflection_result/finalize_ready/done 事件
        """
        config = {"configurable": {"thread_id": thread_id}}

        async for event in self.graph.astream(None, config):
            # 并行节点完成，等待合并
            if "generate_title" in event or "generate_tags" in event:
                pass

            # 合并节点完成
            elif "merge_title_tags" in event:
                yield {"type": "phase", "data": {"phase": "executing", "step": "merged"}}

            # 摘要节点完成
            elif "generate_summary" in event:
                yield {"type": "phase", "data": {"phase": "executing", "step": "summary_done"}}

            # 正文节点完成
            elif "generate_content" in event:
                yield {"type": "phase", "data": {"phase": "executing", "step": "content_done"}}

            # 评估节点完成
            elif "evaluate" in event:
                node_output = event["evaluate"]
                reflection = node_output.get("reflection")
                revision_count = node_output.get("revision_count", 0)
                if reflection:
                    yield {
                        "type": "reflection_result",
                        "data": {
                            **reflection.model_dump(),
                            "revision_count": revision_count
                        }
                    }

            # 修订节点完成
            elif "revise" in event:
                yield {"type": "phase", "data": {"phase": "revising", "step": "revised"}}

            # 完成节点
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
