"""
写作内容生成服务

职责：根据写作计划生成文章标题、摘要、正文。
"""

from typing import Optional

from langchain_core.output_parsers import StrOutputParser
from langchain_openai import ChatOpenAI

from models.writing_models import WritingPlan


class WritingContentService:
    """写作内容生成服务（标题、摘要、正文）"""

    def __init__(self, llm_writer: ChatOpenAI):
        from config.prompts import get_prompt_manager

        self.llm = llm_writer
        self.prompt_manager = get_prompt_manager()

    async def generate_title(self, plan: WritingPlan) -> str:
        """根据写作计划生成文章标题"""
        prompt = self.prompt_manager.get_title_generation_prompt(plan)

        chain = self.llm | StrOutputParser()
        title = await chain.ainvoke(prompt)

        title = title.strip().strip('"').strip("'")
        if "\n" in title:
            title = title.split("\n")[0].strip()

        return title

    async def generate_summary(self, plan: WritingPlan, title: str) -> str:
        """根据写作计划和标题生成文章摘要"""
        prompt = self.prompt_manager.get_summary_generation_prompt(plan, title)

        chain = self.llm | StrOutputParser()
        summary = await chain.ainvoke(prompt)

        return summary.strip()

    async def generate_content(
        self,
        plan: WritingPlan,
        title: str,
        summary: str,
        references: Optional[list] = None,
    ) -> str:
        """根据写作计划、标题、摘要和参考资料生成文章正文"""
        references_str = ""
        if references:
            references_str = "\n".join(
                [
                    f"- {ref.get('title', '未知')}: {ref.get('summary', '')[:100]}"
                    for ref in references[:5]
                ]
            )

        prompt = self.prompt_manager.get_content_generation_prompt(plan, title, summary, references_str)

        chain = self.llm | StrOutputParser()
        content = await chain.ainvoke(prompt)

        return content.strip()
