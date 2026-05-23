---
name: "smart-chat"
description: "智能对话助手，支持多轮对话和工具调用。当用户进行一般性对话、提问、闲聊或需要综合使用博客功能时调用此 Skill。"
---

# 智能对话 Skill

## 概述

本 Skill 提供**智能对话助手**能力，支持多轮对话和工具调用。可以理解用户意图并自动选择合适的工具完成任务，是用户与博客平台交互的核心入口。适用于一般性对话、综合查询、问题解答和需要调用多种工具完成复杂任务的场景。

## 触发条件

当用户输入涉及以下意图时，调用本 Skill：

- 一般性对话和闲聊（如"你好"、"帮我个忙"）
- 综合性问题（如"这个博客平台有哪些功能？"）
- 无法明确归类到其他 Skill 的请求

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

## 工具选择策略

根据用户意图自动选择合适的工具组合：

| 用户意图 | 推荐工具 |
|---------|---------|
| 搜索文章/内容查询 | `search_articles_by_keyword` |
| 热门推荐 | `get_hot_articles` |
| 查看文章详情 | `get_article_by_id` |
| 搜索博主 | `search_authors_by_keyword` |
| 获取时间 | `get_the_time` |
| 综合查询 | `smart_search_references` |
| 平台功能查询 | `get_category_list` |

## 使用示例

### 普通对话

```
用户：你好，请问这个博客平台主要关注哪些技术领域？
AI：你好！让我查一下博客的分类情况...
[调用 get_category_list()]
根据查询结果，本博客平台主要关注以下技术领域：

📂 **分类**：后端开发、前端开发、AI、数据库、运维

你可以根据这些分类浏览感兴趣的内容！
```

## 多轮对话管理

- 使用 Redis 存储对话历史，支持上下文记忆
- 默认保留最近 20 条消息作为上下文
- 支持会话摘要功能（消息数、剩余 TTL）
- 用户可随时清空对话历史

## 注意事项

- 对于明确的搜索类请求，优先使用对应的专业工具，而非依赖 LLM 自身知识
- 工具调用结果应整合后再回复用户，避免直接返回原始 JSON
- 多轮对话中需关注上下文连贯性，避免重复介绍已提供的信息
- 当用户提问不明确时，主动引导用户给出更具体的信息
