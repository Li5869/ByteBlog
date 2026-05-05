"""
LLM 工具函数

提供结构化生成等通用 LLM 调用能力，供各 Service 复用。
"""

import json
import asyncio
from typing import Type, TypeVar

from loguru import logger
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, ValidationError

T = TypeVar("T", bound=BaseModel)


async def structured_generate(
    prompt: str,
    output_model: Type[T],
    llm: ChatOpenAI,
    max_retries: int = 3,
) -> T:
    """
    兼容 DeepSeek 的结构化生成方法

    使用 DeepSeek 官方推荐的 json_object 模式（response_format），
    替代 OpenAI 专有的 with_structured_output（底层依赖 function/tool calling，DeepSeek 不支持）。

    Args:
        prompt: 提示词（需在 prompt 中说明输出格式，json_object 模式会强制 LLM 输出合法 JSON）
        output_model: 目标 Pydantic 模型
        llm: 使用的 LLM 实例
        max_retries: 最大重试次数

    Returns:
        解析后的 Pydantic 模型实例
    """
    schema = output_model.model_json_schema()
    properties = schema.get("properties", {})
    schema_lines = ["请严格按照以下 JSON 格式返回，字段说明："]
    for field_name, field_info in properties.items():
        field_type = field_info.get("type", "string")
        field_desc = field_info.get("description", "")
        schema_lines.append(f'  "{field_name}" ({field_type}): {field_desc}')
    schema_str = "\n".join(schema_lines)

    messages = [
        SystemMessage(
            content=f"你是一个专业的技术文章写作助手。请始终返回合法的 JSON 格式数据，不要包含任何额外的说明文字或 markdown 代码块标记。\n\n{schema_str}"
        ),
        HumanMessage(content=prompt),
    ]

    last_error = None
    for attempt in range(max_retries):
        try:
            response = await llm.ainvoke(messages, response_format={"type": "json_object"})
            content = response.content.strip()

            if not content:
                logger.warning(f"[Structured] 第 {attempt + 1} 次尝试返回空响应，准备重试...")
                await asyncio.sleep(1)
                continue

            data = json.loads(content)
            return output_model.model_validate(data)

        except json.JSONDecodeError as e:
            last_error = e
            logger.warning(
                f"[Structured] 第 {attempt + 1} 次 JSON 解析失败: {e}, "
                f"响应内容: {content[:500] if content else '空响应'}"
            )
            if attempt < max_retries - 1:
                await asyncio.sleep(1)
        except ValidationError as e:
            last_error = e
            logger.warning(f"[Structured] 第 {attempt + 1} 次数据验证失败: {e}")
            if attempt < max_retries - 1:
                await asyncio.sleep(1)
        except Exception as e:
            last_error = e
            logger.error(f"[Structured] 第 {attempt + 1} 次调用异常: {e}")
            if attempt < max_retries - 1:
                await asyncio.sleep(2)

    raise ValueError(f"结构化生成失败，已重试 {max_retries} 次。最后错误: {last_error}")
