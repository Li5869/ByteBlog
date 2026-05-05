---
name: "smart-chat"
description: "智能对话助手，支持多轮对话、工具调用和深度思考模式。当用户进行一般性对话、提问、闲聊或需要综合使用博客功能时调用此 Skill。"
---

# 智能对话 Skill

## 概述

本 Skill 提供**智能对话助手**能力，支持多轮对话、工具调用和深度思考模式。可以理解用户意图并自动选择合适的工具完成任务，是用户与博客平台交互的核心入口。适用于一般性对话、综合查询、问题解答和需要调用多种工具完成复杂任务的场景。

## 触发条件

当用户输入涉及以下意图时，调用本 Skill：

- 一般性对话和闲聊（如"你好"、"帮我个忙"）
- 综合性问题（如"这个博客平台有哪些功能？"）
- 无法明确归类到其他 Skill 的请求
- 需要深度推理和分析的问题（如"比较一下 Spring Boot 和 Spring Cloud 的区别"）

## 使用到的工具

本 Skill 可使用所有已注册的工具，根据用户需求动态选择：

| 工具函数 | 用途 | 来源文件 |
|---------|------|---------|
| `search_articles_by_keyword(keyword, limit=5)` | 搜索文章 | `tools/article_tool.py` |
| `get_hot_articles(limit=10)` | 热门文章 | `tools/article_tool.py` |
| `get_article_by_id(article_id)` | 文章详情 | `tools/article_tool.py` |
| `get_article_content_by_id(article_id)` | 文章正文 | `tools/article_tool.py` |
| `search_authors_by_keyword(keyword, limit=5)` | 搜索博主 | `tools/author_tool.py` |
| `get_hot_authors(limit=10)` | 热门博主 | `tools/author_tool.py` |
| `get_author_by_id(author_id)` | 博主详情 | `tools/author_tool.py` |
| `get_category_list()` | 分类列表 | `tools/blog_tool.py` |
| `get_hot_tag_list(limit=20)` | 热门标签 | `tools/blog_tool.py` |
| `search_knowledge_base(query, top_k=5)` | 知识库检索 | `tools/vector_tool.py` |
| `search_external_tech_blogs(query)` | 外部搜索 | `tools/article_tool.py` |
| `get_the_time()` | 获取当前时间 | `tools/common_tool.py` |
| `get_current_user_id()` | 当前用户 ID | `tools/user_tool.py` |
| `smart_search_references(keywords, topic="")` | 智能搜索 | `tools/smart_search_tool.py` |

## 工作流程

```
用户输入
    │
    ▼
┌──────────────────────────────────────────┐
│          意图识别与路由                     │
│                                           │
│  ┌────────────┐  ┌────────────┐         │
│  │ 需要调用工具 │  │ 纯对话回答  │         │
│  └──────┬─────┘  └──────┬─────┘         │
│         │               │                │
│         ▼               ▼                │
│  ┌──────────────────┐  ┌──────────┐     │
│  │ 选择合适工具调用  │  │ LLM 直接  │     │
│  │ (单次或多次)     │  │ 回答      │     │
│  └──────┬───────────┘  └──────────┘     │
│         │                                │
└─────────┼────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────┐
│          结果整合与回答生成                 │
│  - 将工具返回结果格式化                     │
│  - 结合对话历史生成连贯回答                  │
│  - 标注信息来源（如适用）                    │
└──────────────────────────────────────────┘
```

## 两种工作模式

### 普通模式（默认）

使用 LangChain ChatOpenAI + bind_tools 实现流式输出：

1. LLM 分析用户意图，决定是否调用工具
2. 如需调用工具，自动选择合适的工具并生成参数
3. 并行执行所有工具调用（asyncio.gather）
4. 将工具结果返回给 LLM 生成最终回答
5. 支持流式输出 token

### 深度思考模式（deep_thinking=True）

适用于需要复杂推理的场景，如代码分析、技术比较、方案设计：

1. 使用原生 OpenAI 客户端，手动控制消息格式
2. **推理阶段**：输出 `reasoning` 事件，展示 LLM 的思考过程
3. **工具调用阶段**：输出 `tool_call` 事件，展示工具选择和参数
4. **生成阶段**：输出 `token` 事件，展示最终回答
5. 思考过程保存在 Redis 中供前端回看

## 工具选择策略

根据用户意图自动选择合适的工具组合：

| 用户意图 | 推荐工具 |
|---------|---------|
| 搜索文章/内容查询 | `search_articles_by_keyword` |
| 热门推荐 | `get_hot_articles` |
| 查看文章详情 | `get_article_by_id` |
| 搜索博主 | `search_authors_by_keyword` |
| 获取时间 | `get_the_time` |
| 知识问答 | `search_knowledge_base` |
| 综合查询 | `smart_search_references` |
| 平台功能查询 | `get_category_list` + `get_hot_tag_list` |

## 使用示例

### 普通对话

```
用户：你好，请问这个博客平台主要关注哪些技术领域？
AI：你好！让我查一下博客的分类和标签情况...
[调用 get_category_list()]
[调用 get_hot_tag_list(limit=20)]
根据查询结果，本博客平台主要关注以下技术领域：

📂 **分类**：后端开发、前端开发、AI、数据库、运维
🏷️ **热门标签**：Spring Boot、Vue 3、Python、Docker、LangChain

你可以根据这些分类浏览感兴趣的内容！
```

### 深度思考模式

```
用户：比较一下 LangGraph 和 LangChain 的区别
AI：让我思考一下这个问题...
[推理过程展示]
[调用 search_articles_by_keyword("LangGraph", limit=3)]
[调用 search_articles_by_keyword("LangChain", limit=3)]
[调用 search_knowledge_base("LangGraph vs LangChain", top_k=5)]

根据我的分析，LangGraph 和 LangChain 的主要区别如下：

| 维度 | LangGraph | LangChain |
|------|-----------|-----------|
| 定位 | 图编排运行时引擎 | 应用框架与组件库 |
| 抽象层级 | 底层 | 上层 |
| 核心模式 | StateGraph 节点边 | 链式调用 |
...
```

## 多轮对话管理

- 使用 Redis 存储对话历史，支持上下文记忆
- 默认保留最近 20 条消息作为上下文
- 支持会话摘要功能（消息数、剩余 TTL）
- 用户可随时清空对话历史

## 注意事项

- 对于明确的搜索类请求，优先使用对应的专业工具，而非依赖 LLM 自身知识
- 深度思考模式会展示推理过程，适合复杂分析问题
- 工具调用结果应整合后再回复用户，避免直接返回原始 JSON
- 多轮对话中需关注上下文连贯性，避免重复介绍已提供的信息
- 当用户提问不明确时，主动引导用户给出更具体的信息
