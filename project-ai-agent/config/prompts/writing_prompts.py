"""
WritingAgent 提示词
文件：project-ai-agent/config/prompts/writing_prompts.py
"""

from typing import Optional
from models.writing_models import WritingPlan


def get_writing_plan_prompt(user_request: str) -> str:
    """
    获取写作计划生成提示词

    Args:
        user_request: 用户写作需求

    Returns:
        提示词
    """
    return f"""你是一位专业的内容策划师，擅长将写作需求转化为结构化的写作计划。

用户会给你一个写作需求，请分析并生成写作计划。

注意：
- key_points 要具体，不要泛泛而谈，3-5个核心要点
- writing_style 从以下选项中选择：教程、科普、经验分享、深度分析、技术解读、随笔
- estimated_length 从以下选项中选择：短文(800字)、中文(1500字)、长文(3000字+)
- structure 中的每个元素就是文章的一个章节标题
- reference_keywords 要选择能搜索到高质量技术文章的关键词，3-5个

用户写作需求：
{user_request}"""


def get_writing_revision_prompt(user_request: str, original_plan: Optional[WritingPlan], feedback: str) -> str:
    """
    获取写作计划修改提示词

    Args:
        user_request: 用户原始需求
        original_plan: 原始计划
        feedback: 用户修改意见

    Returns:
        提示词
    """
    original_plan_str = original_plan.model_dump_json() if original_plan else "无"

    return f"""你是一位专业的内容策划师，擅长将写作需求转化为结构化的写作计划。

用户对之前的写作计划提出了修改意见，请根据反馈重新生成写作计划。

原始需求：
{user_request}

原计划：
{original_plan_str}

用户修改意见：
{feedback}

请根据用户反馈调整计划。"""


def get_title_generation_prompt(plan: WritingPlan) -> str:
    """
    获取标题生成提示词

    Args:
        plan: 写作计划

    Returns:
        提示词
    """
    return f"""你是一位专业的文章标题策划师。

请根据以下写作计划，生成5个吸引人的文章标题。

写作计划：
- 主题：{plan.topic}
- 目标读者：{plan.target_audience}
- 核心要点：{', '.join(plan.key_points)}
- 写作风格：{plan.writing_style}

标题要求：
1. 准确反映文章核心内容
2. 吸引目标读者点击
3. 长度控制在15-30个字
4. 适合技术博客场景
5. 每个标题使用不同的风格策略（疑问句/数字列举/痛点切入/对比/故事化）

## 输出格式要求（严格遵守）
- 只输出一个标题（最佳标题）
- 禁止输出任何开场白、解释说明
- 禁止输出"好的，..."、"以下是..."、"我建议..."等废话
- 禁止输出序号、引号等格式标记

直接输出标题："""


def get_summary_generation_prompt(plan: WritingPlan, title: str) -> str:
    """
    获取摘要生成提示词

    Args:
        plan: 写作计划
        title: 文章标题

    Returns:
        提示词
    """
    return f"""请为以下文章撰写摘要。

标题：{title}
主题：{plan.topic}
核心要点：{', '.join(plan.key_points)}
文章结构：{', '.join(plan.structure)}

摘要要求：
1. 长度100-200字
2. 概括文章核心内容
3. 吸引读者阅读全文
4. 不使用"本文"开头，直接切入主题
5. 突出文章的独特价值

## 输出格式要求（严格遵守）
- 只输出摘要文本
- 禁止输出任何开场白、解释说明
- 禁止输出"好的，..."、"以下是摘要..."等废话
- 禁止输出引号等格式标记

直接输出摘要："""


def get_content_generation_prompt(plan: WritingPlan, title: str, summary: str, references_str: str) -> str:
    """
    获取正文生成提示词

    Args:
        plan: 写作计划
        title: 文章标题
        summary: 文章摘要
        references_str: 参考资料字符串

    Returns:
        提示词
    """
    return f"""你是一位专业的技术博客作者。请根据以下写作计划撰写文章正文。

## 写作计划
- 标题：{title}
- 主题：{plan.topic}
- 目标读者：{plan.target_audience}
- 写作风格：{plan.writing_style}
- 文章结构：{', '.join(plan.structure)}
- 预计篇幅：{plan.estimated_length}

## 摘要
{summary}

## 参考资料
{references_str if references_str else "无"}

## 写作要求
1. 严格按文章结构大纲组织内容
2. 每个章节有明确的标题（Markdown ## 格式）
3. 代码示例使用 ```语言 包裹的代码块
4. 重要概念加粗，关键步骤使用有序列表
5. 适当使用引用块（>）标注注意事项或提示
6. 保持写作风格一致

## 输出格式要求（严格遵守）
- 直接输出 Markdown 正文
- 禁止输出任何开场白、结束语、解释说明
- 禁止输出"好的，我来写..."、"以下是文章正文..."等废话
- 禁止输出 ```markdown 等代码块标记
- 第一个字符必须是正文第一个章节标题（## 开头）

现在直接输出正文："""


def get_tags_generation_prompt(plan: WritingPlan, categories_str: str, existing_tags_str: str = "") -> str:
    """
    获取标签和分类生成提示词

    Args:
        plan: 写作计划
        categories_str: 可选分类字符串（带 ID）
        existing_tags_str: 已有标签字符串（带 ID）

    Returns:
        提示词
    """
    existing_tags_section = ""
    if existing_tags_str and existing_tags_str != "无可用标签":
        existing_tags_section = f"""
## 已有标签（优先从中选择）
{existing_tags_str}"""

    return f"""请根据以下文章内容，推荐合适的标签和分类。

## 文章信息
- 主题：{plan.topic}
- 核心要点：{', '.join(plan.key_points)}

## 可选分类（请从中选择一个）
{categories_str}{existing_tags_section}

标签要求：
1. 3-5个标签
2. 每个标签2-8个字
3. 精准反映文章技术主题
4. 优先从已有标签中选择（名称必须完全一致）
5. 如果没有合适的已有标签，可以创建新标签
6. 不与分类名重复"""


def get_writing_evaluation_prompt(plan: WritingPlan, result_content: str, result_title: str, result_summary: str) -> str:
    """
    获取写作质量评估提示词

    Args:
        plan: 写作计划
        result_content: 文章正文
        result_title: 文章标题
        result_summary: 文章摘要

    Returns:
        提示词
    """
    return f"""你是一位严格的技术文章审稿人。请评估以下文章的写作质量。

## 写作计划
- 主题：{plan.topic}
- 目标读者：{plan.target_audience}
- 核心要点：{', '.join(plan.key_points)}
- 文章结构：{', '.join(plan.structure)}

## 文章内容
标题：{result_title}
摘要：{result_summary}

正文：
{result_content}

## 评分标准（严格执行）

| 维度 | 权重 | 评分标准 |
|------|------|----------|
| 完整性 | 30% | 8-10分：覆盖所有核心要点，每个要点有深入阐述<br>5-7分：覆盖大部分要点，阐述深度一般<br>0-4分：遗漏重要要点或阐述严重不足 |
| 结构性 | 20% | 8-10分：章节清晰，逻辑严密，过渡自然<br>5-7分：结构基本合理，偶有跳跃<br>0-4分：结构混乱，逻辑不清 |
| 表达质量 | 25% | 8-10分：语言流畅，术语准确，无冗余<br>5-7分：表达基本通顺，有少量冗余<br>0-4分：语句不通，术语错误，冗余严重 |
| 实用性 | 15% | 8-10分：对目标读者有直接帮助，可操作性强<br>5-7分：有一定参考价值<br>0-4分：实用性差，空洞无物 |
| 格式规范 | 10% | 8-10分：Markdown 格式完美，代码块正确<br>5-7分：格式基本正确，有小瑕疵<br>0-4分：格式混乱 |

## 评估要求
1. 必须阅读完整篇文章后再评分，不要只看开头
2. 对照写作计划中的核心要点，逐一检查是否都有阐述
3. 对照文章结构大纲，检查每个章节是否都有内容
4. 评分要客观公正，不要因为文章长就给高分"""


def get_writing_revision_content_prompt(result_title: str, result_summary: str, result_content: str,
                                        reflection_score: float, weaknesses_str: str, suggestions_str: str) -> str:
    """
    获取文章内容微调提示词

    Args:
        result_title: 文章标题
        result_summary: 文章摘要
        result_content: 文章正文
        reflection_score: 综合评分
        weaknesses_str: 主要不足
        suggestions_str: 改进建议

    Returns:
        提示词
    """
    return f"""你是一位专业的技术文章编辑。请根据审稿意见对文章进行微调。

## 原文章
标题：{result_title}
摘要：{result_summary}
正文：{result_content}

## 审稿意见
- 综合评分：{reflection_score}
- 主要不足：{weaknesses_str}
- 改进建议：{suggestions_str}

## 微调要求
1. 针对审稿意见中的不足进行改进
2. 保持原有的文章结构和风格
3. 不要大幅改动，只做必要的调整

## 输出格式要求（严格遵守）
- 直接输出改进后的完整正文（Markdown 格式）
- 禁止输出任何开场白、结束语、解释说明
- 禁止输出"我来帮你..."、"以下是修改后的..."、"好的，..."等废话
- 禁止输出 ```markdown 等代码块标记
- 第一个字符必须是正文标题（## 开头）

现在直接输出改进后的正文："""
