---
name: "article-search"
description: "搜索博客文章、获取热门文章、查看文章详情和正文内容。当用户查询文章、搜索技术内容或需要推荐阅读时调用此 Skill。"
---

# 文章搜索与推荐 Skill

## 概述

本 Skill 提供博客平台文章的**搜索、推荐和详情查看**能力。涵盖关键词搜索、热门排行、单篇文章详情及正文内容获取。适用于用户查询特定主题文章、浏览热门内容或深度阅读文章的场景。

## 触发条件

当用户输入涉及以下意图时，调用本 Skill：

- 搜索特定主题/关键词的文章（如"搜索关于 Spring Boot 的文章"）
- 查看热门/推荐文章（如"有哪些热门文章"、"推荐几篇文章"）
- 获取某篇文章的详细信息（如"看看这篇文章的详情"）
- 阅读文章正文内容（如"把文章内容读给我听"）

## 使用到的工具

| 工具函数 | 用途 | 来源文件 |
|---------|------|---------|
| `search_articles_by_keyword(keyword, limit=5)` | 根据关键词搜索文章（标题 boost 2.0 + 摘要匹配） | `tools/article_tool.py` |
| `get_hot_articles(limit=10)` | 获取浏览量最高的热门文章 | `tools/article_tool.py` |
| `get_article_by_id(article_id)` | 获取文章基本信息（标题、作者、分类、标签、点赞数、收藏数等） | `tools/article_tool.py` |
| `get_article_content_by_id(article_id)` | 从数据库获取文章正文 Markdown 内容 | `tools/article_tool.py` |
| `get_category_list()` | 获取文章分类列表，用于按分类筛选 | `tools/blog_tool.py` |

## 工作流程

### 场景一：关键词搜索文章

```
用户请求 → search_articles_by_keyword → 格式化结果 → 返回文章列表
```

1. 从用户请求中提取搜索关键词
2. 调用 `search_articles_by_keyword(keyword, limit=5)` 进行搜索
3. 若结果不足且有关联分类/标签信息，可调用 `get_category_list()` 或 `get_hot_tag_list()` 获取辅助信息
4. 整理结果为易读格式（标题、作者、摘要、浏览量、评分）

### 场景二：获取热门文章

```
用户请求 → get_hot_articles → 格式化结果 → 返回热门列表
```

1. 调用 `get_hot_articles(limit=10)` 获取热门文章
2. 按浏览量降序展示
3. 可选择性地提供简要分类或标签说明

### 场景三：查看文章详情

```
用户请求 → get_article_by_id → (可选) get_article_content_by_id → 返回详情
```

1. 从用户请求中提取文章 ID
2. 调用 `get_article_by_id(article_id)` 获取基本信息
3. 若用户需要阅读正文，再调用 `get_article_content_by_id(article_id)` 获取 Markdown 内容
4. 以清晰格式呈现给用户

## 使用示例

### 关键词搜索

```
用户：帮我找找关于 LangGraph 的文章
AI：正在搜索相关文章...
[调用 search_articles_by_keyword("LangGraph", limit=5)]
为您找到以下文章：
1. 📄 《LangGraph 入门指南》 - 作者: 张三 - ⭐ 评分 0.95
   📝 摘要：介绍 LangGraph 的基本概念和使用方法...
2. 📄 《构建智能 Agent 工作流》 - 作者: 李四 - ⭐ 评分 0.88
   📝 摘要：使用 LangGraph 构建复杂 Agent 工作流...
```

### 热门推荐

```
用户：最近有什么热门文章？
AI：正在获取热门文章排行...
[调用 get_hot_articles(limit=10)]
以下是热门文章 TOP 10：
1. 🔥 《Spring Boot 3.0 新特性》 - 浏览 2.3万
2. 🔥 《Vue 3 组合式 API 详解》 - 浏览 1.8万
...
```

### 查看文章详情

```
用户：我想看第3篇文章的详情
AI：请问文章 ID 是多少？
用户：ID 是 12345
AI：正在获取文章详情...
[调用 get_article_by_id(12345)]
📄 《LangGraph 入门指南》
👤 作者：张三
📂 分类：AI
🏷️ 标签：LangGraph, Agent, LLM
👁️ 浏览量：1,234  👍 点赞：56  💬 评论：12
📝 摘要：介绍 LangGraph 的基本概念和使用方法...
需要我为你阅读全文吗？
```

## 注意事项

- 搜索结果按相关性评分降序排列，评分低于 0.5 的结果会在末尾显示
- 热门文章按浏览量排序，仅展示已发布且标记为热门的文章
- 文章正文内容较长，建议先展示标题和摘要，用户确认需要时再获取正文
- 当搜索结果少于 3 条时，可以提示用户换关键词或查看热门文章
