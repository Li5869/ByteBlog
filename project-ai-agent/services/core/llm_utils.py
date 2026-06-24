"""
LLM 工具函数

提供结构化生成等通用 LLM 调用能力，供各 Service 复用。
使用 LangChain 原生 with_structured_output()，由框架处理 JSON Schema 注入和输出解析。

支持两种方法：
- function_calling（默认）：通过 tool_choice="required" 强制模型调用工具，兼容性好
- json_mode：通过 response_format 约束 JSON 输出，与思考模式兼容（DeepSeek 等模型需要）

额外支持：
- streaming_structured_generate：流式生成，支持实时输出思考内容（reasoning_content）
"""

import json
import asyncio
from typing import Any, Callable, Literal, Optional, Type, TypeVar

from loguru import logger
from langchain_core.language_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import BaseModel

T = TypeVar("T", bound=BaseModel)


async def structured_generate(
    prompt: str,
    output_model: Type[T],
    llm: BaseChatModel,
    max_retries: int = 3,
    method: Literal["function_calling", "json_mode"] = "function_calling",
) -> T:
    """
    结构化生成：使用 LangChain 原生 with_structured_output()

    由框架自动处理：
    - JSON Schema 注入（通过 function calling 或 json_mode 传递给模型）
    - 输出解析（自动反序列化为 Pydantic 模型）
    - 类型校验

    Args:
        prompt: 提示词（无需手动描述 JSON 格式，Schema 由框架自动注入）
        output_model: 目标 Pydantic 模型（Field 的 description 会传递给模型）
        llm: LLM 实例（ChatOpenAI / ChatDeepSeek 等）
        max_retries: 最大重试次数
        method: 结构化输出方法
            - "function_calling"（默认）：通过 tool_choice 强制调用工具
            - "json_mode"：通过 response_format 约束 JSON 输出，与思考模式兼容

    Returns:
        解析后的 Pydantic 模型实例
    """
    structured_llm = llm.with_structured_output(output_model, method=method)

    last_error = None
    for attempt in range(max_retries):
        try:
            return await structured_llm.ainvoke(prompt)
        except Exception as e:
            last_error = e
            logger.warning(f"[Structured] 第 {attempt + 1} 次调用失败 (method={method}): {e}")
            if attempt < max_retries - 1:
                await asyncio.sleep(1)

    raise ValueError(f"结构化生成失败，已重试 {max_retries} 次 (method={method})。最后错误: {last_error}")


async def streaming_structured_generate(
    prompt: str,
    output_model: Type[T],
    llm: BaseChatModel,
    writer: Callable[[dict], None],
    event_type: str = "thinking",
    max_retries: int = 3,
) -> T:
    """
    流式结构化生成：支持实时输出思考内容（reasoning_content）

    与 structured_generate 不同，此函数使用 llm.astream() 进行流式调用，
    可以在生成过程中实时输出思考内容到前端。

    Args:
        prompt: 提示词
        output_model: 目标 Pydantic 模型
        llm: LLM 实例（需支持 thinking 模式，如 ChatDeepSeek）
        writer: StreamWriter 回调函数，用于发送 SSE 事件
        event_type: 思考内容的事件类型（默认 "thinking"）
        max_retries: 最大重试次数

    Returns:
        解析后的 Pydantic 模型实例
    """
    # 获取 JSON Schema 用于提示模型输出格式
    schema = output_model.model_json_schema()
    schema_str = json.dumps(schema, ensure_ascii=False, indent=2)

    # 构建包含 JSON Schema 说明的完整 prompt
    # 注意：不使用 bind(response_format) 以确保思考模式的 reasoning_content 能正常传递
    full_prompt = f"""{prompt}

请严格按照以下 JSON Schema 输出响应，确保输出是有效的 JSON 格式：
```json
{schema_str}
```

只输出 JSON，不要包含任何其他文本。"""

    last_error = None
    for attempt in range(max_retries):
        try:
            # 使用 astream 流式调用
            full_content = ""
            thinking_content = ""

            async for chunk in llm.astream([HumanMessage(content=full_prompt)]):
                # 提取思考内容（DeepSeek 特有字段，在 additional_kwargs 中）
                if hasattr(chunk, 'additional_kwargs') and 'reasoning_content' in chunk.additional_kwargs:
                    rc = chunk.additional_kwargs['reasoning_content']
                    if rc:
                        thinking_content += rc
                        # 实时输出思考内容到前端（格式与 thinking 事件一致）
                        writer({"type": event_type, "data": rc})

                # 提取最终内容
                if chunk.content:
                    full_content += chunk.content

            # 解析 JSON 响应为 Pydantic 模型
            # 尝试提取 JSON 内容（可能被 markdown 代码块包裹）
            json_content = full_content.strip()
            if json_content.startswith("```json"):
                json_content = json_content[7:]
            if json_content.startswith("```"):
                json_content = json_content[3:]
            if json_content.endswith("```"):
                json_content = json_content[:-3]
            json_content = json_content.strip()

            # 解析 JSON
            parsed = json.loads(json_content)
            return output_model.model_validate(parsed)

        except Exception as e:
            last_error = e
            logger.warning(f"[StreamingStructured] 第 {attempt + 1} 次调用失败: {e}")
            if attempt < max_retries - 1:
                await asyncio.sleep(1)

    raise ValueError(f"流式结构化生成失败，已重试 {max_retries} 次。最后错误: {last_error}")
