"""
写作助手工具
文件：project-ai-agent/tools/writing_tool.py

提供 SmartAgent 调用写作功能的 4 个工具，控制 WritingAgent 完整生命周期。

工具设计原则：
- writing_start:  创建任务 + 启动异步计划生成，立即返回
- writing_status: 查询任务状态，plan_ready 时返回完整计划内容
- writing_action: 统一处理用户操作（确认/修改/取消）
- writing_result: 获取成果链接
"""

import json

from langchain_core.tools import tool
from loguru import logger

from config.settings import get_settings
from services.business.writing.writing_task_service import get_writing_task_service
from services.business.writing.writing_execution_service import (
    start_plan_phase,
    start_execute_phase,
    start_revise_phase,
)
from services.business.blog_service import get_blog_service
from tools.user_tool import current_user_id


def _generate_result_url(task_id: str) -> str:
    settings = get_settings()
    return f"{settings.frontend_base_url}/writing/{task_id}/result"


def _parse_json_field(value) -> list:
    """解析可能是 JSON 字符串或列表的字段"""
    if isinstance(value, list):
        return value
    if isinstance(value, str) and value:
        try:
            return json.loads(value)
        except (json.JSONDecodeError, TypeError):
            return [value]
    return []


def _format_plan(plan_data: dict) -> str:
    """将计划数据格式化为可读文本"""
    if not plan_data:
        return ""

    # Java 端返回的字段使用 camelCase
    topic = plan_data.get("topic", "未指定")
    writing_style = plan_data.get("writingStyle", "技术教程")
    estimated_length = plan_data.get("estimatedLength", "未指定")
    target_audience = plan_data.get("targetAudience", "中级开发者")
    structure = _parse_json_field(plan_data.get("structure"))
    key_points = _parse_json_field(plan_data.get("keyPoints"))
    keywords = _parse_json_field(plan_data.get("referenceKeywords"))

    lines = [
        f"**主题**：{topic}",
        f"**风格**：{writing_style}",
        f"**预估字数**：{estimated_length}",
        f"**目标读者**：{target_audience}",
        "",
    ]

    outline = structure or key_points
    if outline:
        lines.append("**大纲**：")
        for i, item in enumerate(outline, 1):
            lines.append(f"{i}. {item}")
        lines.append("")

    if keywords:
        lines.append(f"**关键词**：{', '.join(keywords)}")
        lines.append("")

    return "\n".join(lines)


# ==================== 写作工具（4个） ====================

@tool
async def writing_start(user_request: str) -> str:
    """
    启动写作任务。

    创建写作任务并启动异步计划生成流程。
    任务将在后台执行，用户可通过前端进度面板查看实时进度。

    重要：
    - 此工具会立即返回，不会等待计划生成完成
    - 返回后请告知用户"写作任务已启动，请通过下方进度面板查看实时进度"
    - 不要轮询 writing_status 查询进度，进度面板会自动更新

    Args:
        user_request: 用户的写作需求描述，应包含主题、风格、字数等信息

    Returns:
        任务ID和启动状态
    """
    try:
        task_service = get_writing_task_service()
        user_id = current_user_id.get()
        user_id_int = int(user_id) if user_id else None

        # 1. 创建任务
        task_id = await task_service.create_task(user_id_int, user_request)
        if not task_id:
            return "创建写作任务失败，请稍后重试。"

        # 2. 启动异步计划生成（后台执行，不阻塞）
        success = await start_plan_phase(str(task_id), user_request)
        if not success:
            return f"任务 `{task_id}` 已创建，但启动计划生成失败，请重试。"

        # 3. 返回结果（嵌入触发标记，前端检测后显示进度面板）
        trigger = f'{{"action": "start_plan", "task_id": "{task_id}"}}'
        return (
            f"写作任务已启动！\n\n"
            f"**任务ID**: `{task_id}`\n\n"
            f"正在生成写作计划，请通过下方进度面板查看实时进度。\n"
            f"计划生成完成后我会通知您。\n\n"
            f"<!-- WRITING_TRIGGER: {trigger} -->\n"
        )
    except Exception as e:
        logger.error(f"[WritingTool] 启动写作任务异常: {e}")
        return f"启动写作任务时发生错误: {str(e)}"


@tool
async def writing_status(task_id: str) -> str:
    """
    查询写作任务状态。

    获取任务的当前状态。对于进行中的任务，提示用户查看进度面板。

    重要：
    - 不要轮询此工具查询进度，前端进度面板会自动实时更新
    - 仅在以下情况调用：
      a) 用户主动询问进度时
      b) 检测到 plan_ready 后需要获取计划内容时
      c) 需要确认任务是否已完成时

    Args:
        task_id: 写作任务ID

    Returns:
        任务状态信息
    """
    try:
        task_service = get_writing_task_service()
        task_info = await task_service.get_task(int(task_id))

        if not task_info:
            return f"未找到任务 {task_id}，请确认任务ID是否正确。"

        status = task_info.get("status", "unknown")

        # 进行中状态 → 提示用户查看进度面板
        if status in ("planning", "executing", "reflecting"):
            return (
                f"📋 任务 `{task_id}` 正在进行中，请通过下方进度面板查看实时进度。\n"
                f"我会在任务完成后通知您。"
            )

        # 错误/取消状态
        if status in ("error", "cancelled"):
            return f"❌ 任务 `{task_id}` 已{ '出错' if status == 'error' else '取消' }，请通过进度面板查看详情。"

        # plan_ready 时返回完整计划内容
        if status == "plan_ready":
            plan_data = await task_service.get_latest_plan(int(task_id))
            return _format_plan(plan_data) if plan_data else "📋 **任务状态**：计划已生成，等待确认"

        # finalized → 提示用户获取成果链接
        if status == "finalized":
            return (
                f"✅ 任务 `{task_id}` 已完成！\n\n"
                f"请调用 writing_result(task_id=\"{task_id}\") 获取成果链接。"
            )

        return f"📋 任务 `{task_id}` 当前状态：{status}"
    except Exception as e:
        logger.error(f"[WritingTool] 获取任务状态异常: {e}")
        return f"获取任务状态时发生错误: {str(e)}"


@tool
async def writing_action(task_id: str, action: str, feedback: str = "") -> str:
    """
    对写作任务执行用户操作。

    Args:
        task_id: 写作任务ID
        action: 操作类型
            - "approve": 确认计划，开始写作
            - "revise": 修改计划（需提供 feedback）
            - "cancel": 取消任务
        feedback: 修改意见（仅 action="revise" 时需要）

    Returns:
        操作结果
    """
    try:
        if action == "approve":
            success = await start_execute_phase(task_id)
            if success:
                trigger = f'{{"action": "start_execute", "task_id": "{task_id}"}}'
                return (
                    f"✍️ **写作任务已启动！**\n\n"
                    f"📝 任务ID: `{task_id}`\n\n"
                    f"正在生成文章，请通过下方进度面板查看实时进度。\n"
                    f"写作完成后我会通知您。\n\n"
                    f"<!-- WRITING_TRIGGER: {trigger} -->\n"
                )
            return "启动写作任务失败，请确认计划是否存在。"

        elif action == "revise":
            if not feedback:
                return "修改计划时需要提供修改意见，请告诉我您想如何调整。"
            success = await start_revise_phase(task_id, feedback)
            if success:
                return f"正在根据您的反馈修改写作计划...\n\n**任务ID**: `{task_id}`\n\n请稍候，修改完成后会通知您。"
            return "启动计划修改失败，请稍后重试。"

        elif action == "cancel":
            task_service = get_writing_task_service()
            await task_service.update_status(int(task_id), "cancelled", None)
            return f"写作任务 `{task_id}` 已取消。"

        else:
            return f"不支持的操作: `{action}`，请使用 approve（确认）、revise（修改）或 cancel（取消）。"

    except Exception as e:
        logger.error(f"[WritingTool] 执行操作异常: {e}")
        return f"执行操作时发生错误: {str(e)}"


@tool
async def writing_result(task_id: str) -> str:
    """
    获取写作任务成果链接。

    当任务状态为 finalized 时，返回文章预览/编辑页面链接。

    Args:
        task_id: 写作任务ID

    Returns:
        成果链接和文章摘要
    """
    try:
        task_service = get_writing_task_service()
        task_info = await task_service.get_task(int(task_id))

        if not task_info:
            return f"未找到任务 {task_id}，请确认任务ID是否正确。"

        status = task_info.get("status", "unknown")
        if status != "finalized":
            return f"任务 {task_id} 当前状态为 {status}，尚未完成写作。请等待写作完成后再获取成果链接。"

        result_url = _generate_result_url(task_id)
        return (
            f"✅ **您的文章已完成！**\n\n"
            f"🔗 **点击查看完整文章**：{result_url}\n\n"
            f"您可以：\n"
            f"- 查看并编辑文章内容\n"
            f"- 直接发布到博客\n"
            f"- 保存为草稿\n\n"
            f"如需修改，请告诉我您的具体要求。"
        )
    except Exception as e:
        logger.error(f"[WritingTool] 获取成果链接异常: {e}")
        return f"获取成果链接时发生错误: {str(e)}"


@tool
async def writing_publish(task_id: str, action: str) -> str:
    """
    发布或保存写作任务生成的文章。

    用户确认后，获取草稿数据并调用发布接口。

    重要：
    - 调用前必须先向用户确认是发布还是保存草稿
    - action 参数必须是 "publish" 或 "draft"

    Args:
        task_id: 写作任务ID
        action: 操作类型
            - "publish": 直接发布文章（status=1）
            - "draft": 保存为草稿（status=0）

    Returns:
        发布结果，包含文章ID和状态
    """
    try:
        if action not in ("publish", "draft"):
            return f"不支持的操作: `{action}`，请使用 publish（发布）或 draft（保存草稿）。"

        task_service = get_writing_task_service()
        blog_service = get_blog_service()

        task_info = await task_service.get_task(int(task_id))
        if not task_info:
            return f"未找到任务 {task_id}，请确认任务ID是否正确。"

        status = task_info.get("status", "unknown")
        if status != "finalized":
            return f"任务 {task_id} 当前状态为 {status}，尚未完成写作。请等待写作完成后再发布。"

        draft = await task_service.get_draft(int(task_id))
        if not draft:
            return f"未找到任务 {task_id} 的草稿数据，请确认写作是否完成。"

        logger.info(f"[WritingTool] 获取草稿数据: {draft}")

        title = draft.get("title", "")
        if not title or not title.strip():
            logger.error(f"[WritingTool] 草稿标题为空, draft={draft}")
            return f"草稿数据异常：文章标题为空，请确认写作任务是否正常完成。"

        user_id = current_user_id.get()
        if not user_id:
            return "无法获取用户信息，请确认登录状态。"

        tag_ids = []
        if draft.get("tagIds"):
            tag_ids = [int(tid.strip()) for tid in draft["tagIds"].split(",") if tid.strip()]

        tag_names = []
        if draft.get("tagNames"):
            tag_names = [name.strip() for name in draft["tagNames"].split(",") if name.strip()]

        article_data = {
            "title": title,
            "summary": draft.get("summary", ""),
            "content": draft.get("content", ""),
            "cover": draft.get("cover", ""),
            "categoryId": draft.get("categoryId"),
            "tagIds": tag_ids,
            "tagNames": tag_names,
            "status": 1 if action == "publish" else 0,
            "taskId": int(task_id),
        }

        logger.info(f"[WritingTool] 发布文章数据: userId={user_id}, articleData={article_data}")

        result = await blog_service.publish_article(int(user_id), article_data)
        if not result:
            logger.error(f"[WritingTool] 发布文章返回None, userId={user_id}, articleData={article_data}")
            return f"发布文章失败，请稍后重试。"

        article_id = result.get("id")
        article_status = result.get("status")
        status_text = "已发布" if article_status == 1 else "已保存为草稿"

        settings = get_settings()
        article_url = f"{settings.frontend_base_url}/article/{article_id}"

        return (
            f"✅ **文章{status_text}！**\n\n"
            f"📝 **文章ID**: `{article_id}`\n\n"
            f"🔗 **点击查看文章**：{article_url}\n\n"
            f"{'🎉 恭喜！您的文章已成功发布到博客！' if action == 'publish' else '📝 文章已保存为草稿，您可以随时编辑和发布。'}"
        )
    except Exception as e:
        logger.error(f"[WritingTool] 发布文章异常: {e}")
        return f"发布文章时发生错误: {str(e)}"


WRITING_TOOLS = [writing_start, writing_status, writing_action, writing_result, writing_publish]
