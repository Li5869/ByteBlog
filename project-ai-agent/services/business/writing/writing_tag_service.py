"""
写作标签分类服务

职责：根据写作计划生成文章标签和分类，并与数据库中已有的分类/标签进行匹配。
"""

import asyncio
from typing import Optional

from langchain_openai import ChatOpenAI
from loguru import logger

from models.writing_models import WritingPlan, TagGenerationResponse
from services.business.blog_service import get_blog_service
from services.core.llm_utils import structured_generate


class WritingTagService:
    """写作标签分类服务"""

    def __init__(self, llm_classifier: ChatOpenAI):
        from config.prompts import get_prompt_manager

        self.llm = llm_classifier
        self.prompt_manager = get_prompt_manager()

    async def generate_tags(self, plan: WritingPlan) -> dict:
        """
        生成标签和分类，并与数据库中已有的分类/标签进行匹配

        Returns:
            {
                "category_name": "分类名",
                "category_id": 1,  # 数据库中存在则返回 ID，否则为 None
                "tag_names": ["新标签1", "新标签2"],  # 数据库中不存在的标签名
                "tag_ids": [1, 2, 3]  # 数据库中存在的标签 ID
            }
        """
        categories, existing_tags = await self._fetch_existing_data()

        category_list_str = (
            "\n".join(
                [f"- ID: {cat.get('id')}, 名称: {cat.get('name')}" for cat in categories]
            )
            if categories
            else "无可用分类"
        )

        tag_list_str = (
            "\n".join(
                [f"- ID: {tag.get('id')}, 名称: {tag.get('name')}" for tag in existing_tags[:50]]
            )
            if existing_tags
            else "无可用标签"
        )

        prompt = self.prompt_manager.get_tags_generation_prompt(plan, category_list_str, tag_list_str)

        try:
            llm_result: TagGenerationResponse = await structured_generate(
                prompt, TagGenerationResponse, self.llm
            )

            category_name = llm_result.category
            tag_names_from_llm = llm_result.tags

            category_id = self._match_category(category_name, categories)
            tag_ids, new_tag_names = self._match_tags(tag_names_from_llm, existing_tags)

            result = {
                "category_name": category_name,
                "category_id": category_id,
                "tag_names": new_tag_names,
                "tag_ids": tag_ids,
            }

            logger.info(
                f"[Tags] 生成结果: category_id={category_id}, tag_ids={tag_ids}, new_tags={new_tag_names}"
            )
            return result

        except Exception as e:
            logger.error(f"生成标签失败: {e}")
            return {
                "category_name": None,
                "category_id": None,
                "tag_names": [],
                "tag_ids": [],
            }

    async def _fetch_existing_data(self) -> tuple:
        """并发获取数据库中已有的分类和标签列表"""
        blog_service = get_blog_service()

        try:
            categories_task = blog_service.get_categories()
            tags_task = blog_service.get_tags()

            categories_result, tags_result = await asyncio.gather(
                categories_task, tags_task, return_exceptions=True
            )

            categories = categories_result if isinstance(categories_result, list) else []
            existing_tags = tags_result if isinstance(tags_result, list) else []

            if isinstance(categories_result, Exception):
                logger.warning(f"获取分类列表失败: {categories_result}")
                categories = []
            if isinstance(tags_result, Exception):
                logger.warning(f"获取标签列表失败: {tags_result}")
                existing_tags = []

            return categories, existing_tags

        except Exception as e:
            logger.warning(f"获取分类或标签列表失败: {e}")
            return [], []

    @staticmethod
    def _match_category(category_name: Optional[str], categories: list) -> Optional[int]:
        """在已有分类中匹配分类名称，返回分类 ID"""
        if not category_name or not categories:
            return None
        for cat in categories:
            if cat.get("name") == category_name:
                return int(cat.get("id"))
        return None

    @staticmethod
    def _match_tags(tag_names_from_llm: Optional[list], existing_tags: list) -> tuple:
        """在已有标签中匹配标签名称，分离出已有标签 ID 和新标签名"""
        if not tag_names_from_llm:
            return [], []

        if not existing_tags:
            return [], tag_names_from_llm

        existing_tag_map = {tag.get("name"): int(tag.get("id")) for tag in existing_tags}
        tag_ids = []
        new_tag_names = []

        for tag_name in tag_names_from_llm:
            if tag_name in existing_tag_map:
                tag_ids.append(existing_tag_map[tag_name])
            else:
                new_tag_names.append(tag_name)

        return tag_ids, new_tag_names
