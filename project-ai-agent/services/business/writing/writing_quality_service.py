"""
写作质量评估与修订服务

职责：评估文章质量，根据评估结果微调文章内容。
"""

from langchain_core.output_parsers import StrOutputParser
from langchain_openai import ChatOpenAI

from models.writing_models import WritingPlan, WritingResult, ReflectionResult
from services.core.llm_utils import structured_generate


class WritingQualityService:
    """写作质量评估与修订服务"""

    def __init__(self, llm_critic: ChatOpenAI):
        from config.prompts import get_prompt_manager

        self.llm = llm_critic
        self.prompt_manager = get_prompt_manager()

    async def evaluate(self, plan: WritingPlan, result: WritingResult) -> ReflectionResult:
        """评估写作质量，返回包含评分和改进建议的评估结果"""
        prompt = self.prompt_manager.get_writing_evaluation_prompt(
            plan, result.content, result.title, result.summary
        )

        reflection: ReflectionResult = await structured_generate(prompt, ReflectionResult, self.llm)

        return reflection

    async def revise(
        self,
        result: WritingResult,
        reflection: ReflectionResult,
    ) -> WritingResult:
        """根据评估结果微调文章内容"""
        weaknesses_str = "、".join(reflection.weaknesses)
        suggestions_str = "、".join(reflection.suggestions)

        prompt = self.prompt_manager.get_writing_revision_content_prompt(
            result.title,
            result.summary,
            result.content,
            reflection.score,
            weaknesses_str,
            suggestions_str,
        )

        chain = self.llm | StrOutputParser()
        revised_content = await chain.ainvoke(prompt)

        return result.model_copy(update={"content": revised_content.strip()})
