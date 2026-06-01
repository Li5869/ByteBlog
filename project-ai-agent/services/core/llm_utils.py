"""
LLM 工具函数

提供结构化生成等通用 LLM 调用能力，供各 Service 复用。
使用 LangChain 原生 with_structured_output()，由框架处理 JSON Schema 注入和输出解析。
"""

import asyncio
from typing import Type, TypeVar

from loguru import logger
from langchain_core.language_models import BaseChatModel
from pydantic import BaseModel

T = TypeVar("T", bound=BaseModel)


async def structured_generate(
    prompt: str,
    output_model: Type[T],
    llm: BaseChatModel,
    max_retries: int = 3,
) -> T:
    """
    结构化生成：使用 LangChain 原生 with_structured_output()

    由框架自动处理：
    - JSON Schema 注入（通过 function calling 传递给模型）
    - 输出解析（自动反序列化为 Pydantic 模型）
    - 类型校验

    Args:
        prompt: 提示词（无需手动描述 JSON 格式，Schema 由框架自动注入）
        output_model: 目标 Pydantic 模型（Field 的 description 会传递给模型）
        llm: LLM 实例（ChatOpenAI / ChatDeepSeek 等）
        max_retries: 最大重试次数

    Returns:
        解析后的 Pydantic 模型实例
    """
    structured_llm = llm.with_structured_output(output_model, method="function_calling")

    last_error = None
    for attempt in range(max_retries):
        try:
            return await structured_llm.ainvoke(prompt)
        except Exception as e:
            last_error = e
            logger.warning(f"[Structured] 第 {attempt + 1} 次调用失败: {e}")
            if attempt < max_retries - 1:
                await asyncio.sleep(1)

    raise ValueError(f"结构化生成失败，已重试 {max_retries} 次。最后错误: {last_error}")
