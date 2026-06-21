"""
搜索专家 Agent（基于 langchain.agents.create_agent）

作为 Sub-Agent 通过 @tool 注册到 Supervisor (SmartAgent)，
Supervisor 通过 LLM tool-calling 自主决定是否调用搜索专家。

使用 DeepSeek Chat 模型（非思考模式），适合搜索场景的快速响应。
"""

from typing import Optional, Callable
from uuid import uuid4

from langchain.agents import create_agent
from langchain_core.messages import AIMessage
from langchain_deepseek import ChatDeepSeek
from langgraph.checkpoint.memory import MemorySaver
from loguru import logger

from config.prompts import get_prompt_manager
from config.settings import get_settings
from tools.article_tool import search_articles_by_keyword, get_hot_articles, search_external_tech_blogs
from tools.author_tool import search_authors_by_keyword, get_hot_authors, get_author_by_id
from tools.blog_tool import get_category_list
from tools.web_scraper_tool import scrape_webpage
from tools.firecrawl_tool import firecrawl_scrape, firecrawl_search


class SearchAgent:
    """
    搜索专家 Agent — 基于 create_agent 的 ReAct 范式

    拥有搜索相关工具，作为 Sub-Agent 被 Supervisor 调用。
    使用 DeepSeek Chat 模型（非思考模式），temperature=0.1 保证搜索精确性。
    """

    def __init__(self):
        settings = get_settings()

        self.llm = ChatDeepSeek(
            model=settings.model_name_deepseek,
            api_key=settings.openai_api_key_deepseek,
            streaming=True,
            extra_body={"thinking": {"type": "disabled"}},
        )

        self.tools = self._build_tools()
        self.system_prompt = get_prompt_manager().get_search_agent_system_prompt()
        self.agent = create_agent(
            model=self.llm,
            tools=self.tools,
            system_prompt=self.system_prompt,
            checkpointer=MemorySaver(),
            name="search_agent",
        )

    def _build_tools(self) -> list:
        """构建搜索工具列表"""
        tools = [
            search_articles_by_keyword,
            get_hot_articles,
            search_authors_by_keyword,
            get_hot_authors,
            get_author_by_id,
            get_category_list,
            scrape_webpage,
            search_external_tech_blogs,
            firecrawl_scrape,
            firecrawl_search,
        ]
        return tools

    # ==================== 对外接口 ====================

    async def execute(self, task: str, stream_writer: Optional[Callable] = None) -> str:
        """
        被 Supervisor 调用的入口，返回字符串结果

        Args:
            task: 搜索任务描述（Supervisor 已包含足够上下文）
            stream_writer: Supervisor 的流式写入器，用于转发 Sub-Agent 内部事件到前端

        Returns:
            搜索结果字符串
        """
        thread_id = f"search_{uuid4()}"

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
                                        "agent": "search_agent",
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

            return "搜索未找到相关结果"

        except Exception as e:
            logger.error(f"[SearchAgent] 执行失败: {e}")
            return f"搜索执行失败: {str(e)}"


# ==================== 单例 ====================

_search_agent: Optional[SearchAgent] = None


def get_search_agent() -> SearchAgent:
    """获取搜索专家 Agent 单例"""
    global _search_agent
    if _search_agent is None:
        _search_agent = SearchAgent()
    return _search_agent
