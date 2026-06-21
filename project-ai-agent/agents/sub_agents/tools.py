"""
Sub-Agent 工具注册

将各专业 Sub-Agent 包装为 @tool，Supervisor 通过 LLM tool-calling 自主决定调用哪个 Sub-Agent。

基于 LangGraph 官方推荐的 Supervisor (tool-calling) 模式：
https://langchain-ai.github.io/langgraph/concepts/multi_agent/#supervisor-tool-calling

注意：不使用 InjectedState，因为 SmartAgent 的 _tool_executor_node 手动调用 tool.ainvoke()，
InjectedState 仅在 LangGraph 的 ToolNode / create_react_agent 中自动生效。
Supervisor 负责在 task 参数中提供足够上下文。

流式转发：Sub-Agent 的 execute 方法接受 stream_writer 参数，
将内部工具调用事件转发到 Supervisor 的 stream_writer，最终显示在前端思考块中。
"""

from langchain_core.tools import tool
from langgraph.config import get_stream_writer
from typing import Annotated

from agents.sub_agents.search_agent import get_search_agent
from agents.sub_agents.knowledge_agent import get_knowledge_agent
from agents.sub_agents.code_execution_agent import get_code_execution_agent


# ==================== 搜索专家 ====================

@tool
async def search_agent(
    task: Annotated[str, "需要搜索的具体任务描述，如'搜索微服务相关文章'"],
) -> str:
    """
    搜索专家：擅长博客文章搜索、博主搜索、外部技术博客搜索、网页爬取、分类查询。

    适用场景：
    - 用户想搜索博客文章或查找某类文章
    - 用户想搜索博主或查看热门博主
    - 需要搜索外部技术博客资源
    - 需要爬取某个网页的内容
    - 需要查看文章分类列表

    调用此工具时，请给出清晰具体的搜索任务描述。
    """
    agent = get_search_agent()
    writer = get_stream_writer()
    result = await agent.execute(task=task, stream_writer=writer)
    return result


# ==================== 知识库专家 ====================

@tool
async def knowledge_agent(
    task: Annotated[str, "需要查询的知识库任务描述，如'项目如何实现分布式锁'"],
) -> str:
    """
    知识库专家：擅长 RAG 知识库检索，支持项目知识和面试知识分类查询。

    适用场景：
    - 用户询问项目实现细节、系统架构、代码逻辑
    - 用户准备面试，询问技术原理、底层机制
    - 需要从知识库中查找技术文档内容

    调用此工具时，请给出清晰的知识库查询描述。
    """
    agent = get_knowledge_agent()
    writer = get_stream_writer()
    result = await agent.execute(task=task, stream_writer=writer)
    return result


# ==================== 代码执行专家 ====================

@tool
async def code_execution_agent(
    task: Annotated[str, "需要执行的代码任务描述，如'执行这段 Python 代码'"],
) -> str:
    """
    代码执行专家：擅长执行和验证各种编程语言的代码。

    适用场景：
    - 用户要求执行、运行某段代码
    - 用户问"这段代码输出什么"
    - 用户需要验证代码的正确性
    - 用户需要调试代码，分析执行错误
    - 用户想测试某个算法或代码片段

    支持语言：Python、JavaScript、Java、C++、Go、Rust 等 60+ 语言

    调用此工具时，请包含要执行的代码内容和编程语言。
    """
    agent = get_code_execution_agent()
    writer = get_stream_writer()
    result = await agent.execute(task=task, stream_writer=writer)
    return result
