"""
写作 Agent 专属服务

职责分工：
- WritingContentService: 内容生成（标题、摘要、正文）
- WritingQualityService: 质量评估与修订
- WritingTagService: 标签分类推荐
- WritingTaskService: 任务状态管理
"""

from services.business.writing.writing_content_service import WritingContentService
from services.business.writing.writing_quality_service import WritingQualityService
from services.business.writing.writing_tag_service import WritingTagService
from services.business.writing.writing_task_service import WritingTaskService, get_writing_task_service

__all__ = [
    "WritingContentService",
    "WritingQualityService",
    "WritingTagService",
    "WritingTaskService",
    "get_writing_task_service",
]
