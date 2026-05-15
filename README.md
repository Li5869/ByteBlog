<div align="center">

# ByteBlog

**面向开发者的 AI 增强全栈博客平台**

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.4-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20AI-2.0.0--M4-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring AI">
  <img src="https://img.shields.io/badge/Vue_3-3.5.13-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue 3">
  <img src="https://img.shields.io/badge/Python-3.12+-3776AB?style=flat-square&logo=python&logoColor=white" alt="Python">
  <img src="https://img.shields.io/badge/LangGraph-0.3+-1C3C3C?style=flat-square&logo=langchain&logoColor=white" alt="LangGraph">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/PostgreSQL-16+-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/pgvector-0.2+-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="pgvector">
  <img src="https://img.shields.io/badge/Elasticsearch-8.x-005571?style=flat-square&logo=elasticsearch&logoColor=white" alt="Elasticsearch">
  <img src="https://img.shields.io/badge/Nacos-2.x-00BFFF?style=flat-square&logo=nacos&logoColor=white" alt="Nacos">
</p>

</div>

---

## 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [技术栈](#技术栈)
  - [后端 Java](#后端-java)
  - [AI Agent Python](#ai-agent-python)
  - [前端双端](#前端双端)
- [系统架构](#系统架构)
- [后端模块全景](#后端模块全景)
- [AI Agent 架构](#ai-agent-架构)
  - [写作 Agent 工作流](#写作-agent-工作流)
  - [Smart Agent 工具调用](#smart-agent-工具调用)
  - [RAG 知识库问答](#rag-知识库问答)
- [实时通信体系](#实时通信体系)
- [持久层策略](#持久层策略)
- [环境要求](#环境要求)
- [快速启动](#快速启动)
  - [第一步：基础设施启动](#第一步基础设施启动)
  - [第二步：后端启动](#第二步后端启动)
  - [第三步：AI Agent 启动](#第三步ai-agent-启动)
  - [第四步：前端启动](#第四步前端启动)
- [环境变量全表](#环境变量全表)
  - [Java 后端](#java-后端)
  - [Python AI Agent](#python-ai-agent)
- [部署方式](#部署方式)
- [安全建议](#安全建议)
- [项目结构](#项目结构)
- [API 文档索引](#api-文档索引)
- [License](#license)

---

## 项目简介

ByteBlog 是一个面向开发者的技术博客平台，集文章管理、问答社区、AI 智能写作、全文搜索、实时通信于一体。项目采用 **三层架构**：Java Spring Boot 后端、Python LangGraph AI Agent、Vue 3 双端前端（用户端 + 管理端）。

技术选型对标工业级应用：JDK 21 虚拟线程压榨并发性能、Caffeine + Redis 两级缓存对抗热点、Elasticsearch 毫秒级全文搜索、RabbitMQ 异步解耦削峰、WebSocket + SSE 双通道实时推送、LangGraph 智能 Agent 驱动 AI 写作与对话、Nacos 服务发现与配置中心实现动态配置管理。

---

## 核心特性

| 模块 | 能力 | 实现方式 |
|------|------|----------|
| 🤖 **AI 写作 Agent** | 四阶段工作流（计划→执行→反思→定稿），SSE 流式输出，5 维质量评估自动微调 | LangGraph StateGraph + DeepSeek + Tavily 搜索 |
| 💬 **AI 智能对话** | 6 种工具自动调用（ES 搜文章、pgvector 知识库、博客工具），渐进式披露，深度思考模式 | LangGraph ReAct + SSE |
| 📚 **RAG 知识库** | 文档分块、向量化存储、语义检索，支持管理端上传 | OpenAI Embedding + pgvector |
| 🔍 **全文搜索** | ES 统一搜索（文章/问答/作者）+ Completion Suggester 搜索建议 | Spring Data Elasticsearch |
| 💬 **实时通信** | WebSocket 私信推送 + 心跳保活，SSE 点赞/评论/关注通知 | WebSocket + SseEmitter + MQ |
| ⚡ **高性能** | JDK 21 虚拟线程、Caffeine 本地 + Redis 分布式两级缓存、MQ 异步 | Spring Boot 4 Virtual Threads |
| 🔐 **安全认证** | JWT 双令牌（Access 30min + Refresh 7d Rotation）、API Key 服务间鉴权、方法级权限 | Spring Security + JJWT |
| 🎨 **双前端** | 用户端 NaiveUI 社区风格 + 管理端 Element Plus 后台风格，Tailwind 响应式 | Vue 3 + Vite |
| 📊 **数据可视化** | 管理端 ECharts 仪表盘（用户增长、文章发布趋势统计） | ECharts 6 |
| 🌐 **服务治理** | Nacos 服务发现 + 配置中心，动态配置管理，提示词集中管理 | Spring Cloud Alibaba Nacos |

---

## 技术栈

### 后端 Java

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | 运行时 + 虚拟线程 |
| Spring Boot | 4.0.4 | 主框架 |
| Spring AI | 2.0.0-M4 | 内容审核 / 摘要生成/文章润色 |
| Spring Cloud Alibaba | 2025.1.0.0 | 微服务生态 |
| Nacos | 2.x | 服务发现 + 配置中心 |
| Spring Data Redis / RabbitMQ / Elasticsearch | 内置 | 数据访问 |
| MyBatis Plus | 3.5.16 | ORM（适配 Spring Boot 4） |
| PostgreSQL Driver | 42.7.8 | 数据库驱动 |
| Redisson | 3.45.1 | Redis 分布式锁 / 数据结构 |
| XXL-Job | 2.4.2 | 分布式定时任务调度 |
| JJWT | 0.12.7 | JWT 令牌签发与验证 |
| Aliyun OSS SDK | 3.18.1 | 对象存储（图片附件） |
| Springdoc OpenAPI | 3.0.2 | API 文档自动生成 |
| Caffeine | 3.2.3 | 本地缓存 |
| Hutool | 5.8.40 | 工具库 |
| FastJSON2 | 2.0.56 | JSON 处理 |
| WebSocket / WebFlux | 内置 | 实时通信 |

### AI Agent Python

| 技术 | 版本 | 用途 |
|------|------|------|
| Python | ≥3.12 | 运行时 |
| FastAPI | ≥0.115.0 | Web 框架 + SSE 流式 |
| LangChain | ≥1.2.0 | LLM 调用链 |
| LangGraph | ≥0.3.0 | Agent 有向图工作流 |
| LangChain-OpenAI | ≥1.2.0 | OpenAI / DeepSeek 接入 |
| LangChain-Postgres | ≥0.0.17 | pgvector 检查点存储 |
| LangChain-Tavily | ≥0.2.0 | 外部搜索工具 |
| OpenAI SDK | ≥1.60.0 | 原生客户端 |
| Elasticsearch | ≥8.16.0 | ES 搜索客户端 |
| pgvector | ≥0.2.5, <0.4 | 向量数据库 |
| Redis | ≥5.2.0 | 对话记忆 / 任务状态 |
| psycopg | ≥3.2.0 | PostgreSQL 驱动 |
| httpx | ≥0.28.0 | HTTP 异步客户端 |
| uvicorn | ≥0.34.0 | ASGI 服务器 |
| loguru | ≥0.7.3 | 日志 |

### 前端双端

| 技术 | 用户端 | 管理端 |
|------|--------|--------|
| 框架 | Vue 3.5.13 | Vue 3.5.13 |
| 构建工具 | Vite 6.2.4 | Vite 6.2.4 |
| UI 组件库 | NaiveUI ^2.44.1 | Element Plus ^2.13.6 |
| 状态管理 | Pinia ^3.0.4 | Pinia ^3.0.4 |
| 路由 | Vue Router ^4.5.0 | Vue Router ^4.5.0 |
| CSS | Tailwind CSS ^3.4.17 | Tailwind CSS ^3.4.17 |
| Markdown 编辑器 | Vditor ^3.11.2 | — |
| Markdown 渲染 | marked ^17.0.5 | marked ^17.0.5 |
| 图表 | — | ECharts ^6.0.0 |
| 雪花 ID 精度 | json-bigint ^1.0.0 | json-bigint ^1.0.0 |
| 工具库 | @vueuse/core ^12.8.2 | — |
| 插件 | — | @vitejs/plugin-vue ^5.2.3 |

---

## 系统架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                           用户浏览器                                  │
│  ┌──────────────────────────────┐  ┌──────────────────────────┐     │
│  │  用户端前端 (Vue3 + NaiveUI)  │  │  管理端前端 (Vue3+ElPlus)│     │
│  │  :5173                       │  │  :5174                   │     │
│  └────────────┬─────────────────┘  └────────────┬─────────────┘     │
└───────────────┼──────────────────────────────────┼────────────────────┘
                │ HTTP / WebSocket / SSE           │
                ▼                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│            Spring Boot 后端模块 (:8080/api)                          │
│                                                                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ security │ │ article  │ │ comment  │ │question  │ │  ai      │  │
│  │ 认证授权  │ │ 文章管理  │ │ 评论管理  │ │ 问答社区  │ │ AI 能力  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │interact. │ │  search  │ │  job     │ │  common  │               │
│  │ 社交互动  │ │ 全文搜索  │ │ 定时任务  │ │ 公共设施  │               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
│  ┌──────────┐ ┌──────────┐                                           │
│  │ blog-api │ │blog-app  │                                           │
│  │ 接口契约  │ │ 启动模块  │                                           │
│  └──────────┘ └──────────┘                                           │
└──────┬────────────┬──────────────┬──────────────┬────────────────────┘
       │            │              │              │
       ▼            ▼              ▼              ▼
┌──────────┐ ┌──────────┐  ┌──────────┐  ┌──────────────┐
│PostgreSQL│ │  Redis   │  │ RabbitMQ │  │Elasticsearch  │
│+ pgvector│ │ 缓存/锁  │  │ 异步消息  │  │    全文检索    │
│  :5432   │ │  :6379   │  │  :5672   │  │    :9200      │
└──────────┘ └──────────┘  └──────────┘  └──────────────┘
       │
       ▼
┌──────────────┐
│    Nacos     │
│ 服务发现/配置│
│   :8848      │
└──────────────┘
                      ▲
                      │ HTTP / SSE（API Key 鉴权）
                      ▼
┌──────────────────────────────────────────────────────────────────────┐
│           Python AI Agent 服务 (:8000)                               │
│                                                                      │
│  ┌────────────────────┐  ┌────────────────┐  ┌───────────────────┐  │
│  │  Writing Agent     │  │  Smart Agent   │  │  RAG Agent        │  │
│  │  Plan→Execute→     │  │  ReAct 循环    │  │  文档→分块→向量化  │  │
│  │  Reflect→Finalize  │  │  6 工具调用    │  │  语义检索→LLM 回答 │  │
│  └────────────────────┘  └────────────────┘  └───────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  共享基础设施层                                               │    │
│  │  services/ → LLM/Embedding/Memory/Blog/Task                  │    │
│  │  tools/    → ES搜索/向量检索/作者搜索/分类标签/混合搜索       │    │
│  └──────────────────────────────────────────────────────────────┘    │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                     ┌────────┴────────┐
                     ▼                 ▼
               DeepSeek / OpenAI     Tavily Search
               (LLM 推理 + Embedding) (外部互联网搜索)
```

---

## 后端模块全景

```
project-backen/  —— Spring Boot 4 + Maven 多模块
│
├── blog-common/      # 公共基础设施（MQ、常量、工具类、DTO/VO）
│   ├── api/              跨模块接口契约（TokenValidationApi、FollowerApi）
│   ├── constant/         Redis Key / MQ 队列常量
│   ├── mqHandler/        死信队列重试处理器
│   ├── config/           Redisson/Caffeine/OSS/Jackson/线程池配置
│   └── util/             工具类（MessageUtil 等）
│
├── blog-push/        # 实时推送通道（WebSocket + SSE）
│   ├── websocket/        WebSocket 连接管理 + 心跳 + 消息分发
│   ├── sse/              SseEmitter 通知推送
│   ├── service/          OnlineStateService（Redis 在线状态）、MessagePushService
│   └── vo/               PushMessageVO
│
├── blog-api/         # 模块间接口契约（LikeApi、FollowApi、SearchSyncApi 等）
│
├── blog-security/    # 认证授权
│   ├── JWT 双令牌（Access 30min + Refresh 7d Rotation Rotation）
│   ├── API Key 鉴权体系（内部服务间认证）
│   └── 方法级权限（@PreAuthorize）
│
├── blog-article/     # 文章管理
│   ├── 文章 CRUD + 草稿 + 软删除
│   ├── 分类/标签管理（多对多关系）
│   ├── 轮播图
│   └── 文章详情缓存（Caffeine + Redis 两级）
│
├── blog-comment/     # 评论管理（MQ 异步 + AI 评论总结）
│
├── blog-interaction/ # 社交互动
│   ├── 点赞/收藏/关注（Redis + MQ 异步落库）
│   ├── 私信（WebSocket 实时推送）
│   ├── 通知（SSE + 业务通知/系统通知双队列）
│   └── 浏览历史
│
├── blog-question/    # 问答社区（提问、回答、采纳、标签关联）
│
├── blog-search/      # Elasticsearch 全文搜索
│   ├── 统一搜索（文章/问答/作者三合一）
│   ├── Completion Suggester 搜索建议（FST 毫秒级响应）
│   └── MQ 增量同步（文章发布/更新自动索引）
│
├── blog-ai/          # AI 能力集成
│   ├── AI 对话（Java Spring AI + Python Agent 双引擎）
│   ├── AI 写作 Agent 管理（任务状态/计划/草稿）
│   ├── 内容审核（Spring AI 自动审核）
│   ├── 文章摘要/标题生成/润色（MQ 异步处理）
│   └── Nacos 提示词配置管理（动态配置刷新）
│
├── blog-job/         # XXL-Job 定时任务
│   ├── 点赞/收藏计数落库
│   ├── 浏览历史批量同步
│   └── 更多定时任务可扩展
│
└── blog-application/ # 启动模块（聚合所有子模块 + Spring Boot 入口 + Nacos 配置）
```

---

## AI Agent 架构

Python AI Agent 服务（FastAPI :8000）包含三个独立 Agent，共享底层基础设施层：

```
project-ai-agent/
│
├── main.py               # FastAPI 入口（SSE + CORS + 生命周期管理）
│
├── api/                   # 路由层
│   ├── chat_router.py     # 智能对话（SSE 流式，支持工具调用/深度思考）
│   ├── writing_router.py  # AI 写作（SSE 流式，4 阶段工作流）
│   ├── rag_router.py      # RAG 知识库问答
│   └── knowledge_router.py# 知识库文档上传/管理
│
├── agents/                # LangGraph Agent
│   ├── smart_agent.py     # ReAct 循环：思考→工具调用→观察→继续
│   └── writing_agent.py   # Plan-and-Execute：计划→执行→反思→定稿
│
├── services/              # 业务服务层
│   ├── llm_service.py       # DeepSeek/OpenAI 双模型封装
│   ├── embedding_service.py # OpenAI Embedding 远程调用
│   ├── memory_service.py    # Redis 对话记忆管理
│   ├── blog_service.py      # Java 后端 API 调用（自动注入 API Key）
│   ├── document_service.py  # 文档分块（RecursiveCharacterTextSplitter）
│   └── writing_task_service.py  # 写作任务状态管理
│
├── tools/                 # Agent 工具库
│   ├── article_tool.py      # ES 文章检索（搜索/最新/热门）
│   ├── vector_tool.py       # pgvector 知识库语义检索
│   ├── author_tool.py       # 作者搜索
│   ├── blog_tool.py         # 分类/标签查询
│   └── smart_search_tool.py # ES+Vector 混合搜索
│
├── models/                # Pydantic 数据模型
├── config/                # 配置（Settings + 8个Prompts模板）
├── common/                # 常量/异常/ES基础
└── vectorstore/           # pgvector 向量存储封装
```

### 写作 Agent 工作流

基于 **LangGraph StateGraph** 的 Plan-and-Execute 模式（4 阶段）：

```
阶段 1：Plan（计划）
  • LLM 分析写作需求 → 生成结构化写作计划（主题/目标读者/风格/大纲）
  • 智能搜索参考资料：站内 ES 优先 → 不足 3 条补 Tavily 外部搜索
  • 限定技术社区域名，保证搜索结果质量
  • SSE 推送 plan_ready 事件 → 等待用户确认/修改

阶段 2：Execute（执行）
  • 依次流式输出：标题 → 摘要 → 正文（Markdown 流式）→ 标签
  • 标签匹配：LLM 生成标签名 → 匹配已有 category_id/tag_ids
  • 未匹配的新标签自动分离为 new_tag_names
  • SSE token 事件逐 token 流式推送

阶段 3：Reflect（反思）
  • 5 维质量评估：完整性(30%) + 结构性(20%) + 表达(25%)
                               + 实用性(15%) + 格式(10%)
  • 综合评分 < 7.0 自动进入微调循环（最多 3 次）
  • SSE 推送 reflection_result 事件

阶段 4：Finalize（定稿）
  • Python Agent 将完整内容（含分类/标签 ID）保存到 Java 后端
  • 前端直调 Java 后端接口发布文章或保存草稿
  • SSE 推送 finalize_ready + done 事件
```

**LLM 调用策略：**
- 纯文本输出（标题/摘要/正文）：DeepSeek，temperature=0.3，字符串输出
- 结构化输出（计划/标签/评估）：DeepSeek，temperature=0.3，JSON 格式

### Smart Agent 工具调用

基于 **LangGraph ReAct** 循环，6 个内置工具：

| 工具 | 功能 | 触发场景 |
|------|------|----------|
| ES 文章检索 | 按关键词/分类/标签搜索文章 | "Spring Boot 文章" |
| pgvector 知识库 | 语义检索已上传文档 | 基于文档内容的问答 |
| 作者搜索 | 按用户名搜索作者 | "找一下张三" |
| 分类标签查询 | 查询所有分类和标签 | "有哪些分类" |
| Tavily 搜索 | 外部互联网搜索 | 实时技术问题 |
| 混合搜索 | ES + 向量检索加权融合 | 综合查询 |

**渐进式披露策略：** Agent 在思考过程中逐步展示推理链和工具调用过程，通过 SSE 流式输出给用户，提升对话透明度和可信度。

**深度思考模式：** 使用原生 OpenAI 客户端自行管理消息历史，跳出 LangGraph 循环限制，实现更长的推理链。

### RAG 知识库问答

**文档处理流程：**
1. 管理端上传文档（PDF / Markdown / TXT）
2. 自动分块（RecursiveCharacterTextSplitter，chunk_size=500, overlap=50）
3. OpenAI Embedding 远程向量化
4. 存入 pgvector（数据库级向量检索，相似度搜索）
5. 用户提问 → 语义检索 Top-K → LLM 综合回答

---

## 实时通信体系

ByteBlog 采用 **WebSocket + SSE 双通道**，由独立的 `blog-push` 模块承载：

| 通道 | 用途 | 方向 | 路径 |
|------|------|------|------|
| **WebSocket** | 私信推送、在线状态、心跳检测 | 双向 | `ws://host/ws` |
| **SSE** | 点赞/评论/关注/系统通知推送 | 服务端→客户端 | `GET /interaction/sse/connect` |
| **Redis** | 在线状态管理（心跳 + TTL 自动过期） | — | Key: `online:users` |

**WebSocket 整体流程：**
```
客户端 → JWT 鉴权 → 建立 Session → 踢掉旧连接 → 标记在线 → 心跳保活
                                                          │
                                                          ▼
                    发送私信 → 检查接收方在线 → WebSocket 推送 → 前端弹窗
```

**SSE 通知流程：**
```
产生通知（点赞/评论/关注） → MQ 异步处理 → SseEmitter 推送 → 前端弹窗
```

---

## 持久层策略

| 存储 | 用途 | 策略 |
|------|------|------|
| **PostgreSQL** | 业务数据持久化 | 雪花 ID 主键，MyBatis Plus ORM |
| **pgvector** | 向量数据 | 同一 PostgreSQL 实例的 vector 扩展，余弦相似度搜索 |
| **Caffeine** | 本地缓存（L1） | 文章详情等高频读取热数据，TTL=5min，最大 10k 条 |
| **Redis** | 分布式缓存（L2）+ 计数器 + 在线状态 + 分布式锁 | 热点数据冷启动缓存预热 |
| **RabbitMQ** | 异步解耦 | 点赞/收藏落库、评论通知、文章 ES 同步、AI 异步处理 |
| **Elasticsearch** | 全文搜索 | 文章/问答/作者统一索引，MQ 增量同步 |
| **Nacos** | 服务发现 + 配置中心 | 服务注册发现、动态配置管理、提示词集中管理 |

---

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21（推荐 Oracle JDK 21 / OpenJDK 21） |
| Maven | 3.9+ |
| Node.js | 20+ |
| npm / pnpm | 对应 Node.js 版本 |
| Python | 3.12+ |
| PostgreSQL | 16+（需安装 pgvector 扩展） |
| Redis | 7.x |
| Elasticsearch | 8.x |
| RabbitMQ | 3.x |
| Nacos | 2.x（可选，用于服务发现和配置中心） |

---

## 快速启动

### 第一步：基础设施启动

确保以下服务已运行（可通过 Docker 快速启动）：

- **PostgreSQL** 16+（数据库 `person_blog`，安装 pgvector 扩展）
- **Redis** 7.x
- **Elasticsearch** 8.x
- **RabbitMQ** 3.x
- **Nacos** 2.x（可选，用于服务发现和配置中心）
- **XXL-Job 调度中心**（可选，不配置不影响业务核心功能）

**数据库初始化：**

创建数据库并启用 pgvector 扩展，然后导入建表脚本 `sql/public.sql`。

**Nacos 配置（可选）：**

如果使用 Nacos 配置中心，需要在 Nacos 控制台创建配置文件：
- **Data ID**: `person-blog.yaml`
- **Group**: `DEFAULT_GROUP`（或环境变量 `NACOS_GROUP`）
- **配置格式**: YAML
- **配置内容**: 参考 `project-backen/blog-ai/src/main/resources/nacos-prompt-config.yaml`

### 第二步：后端启动

**1. 配置环境变量**

进入 `project-backen/` 目录，将 `.env.example` 复制为 `.env`，填写所有必需的配置项（参考下方环境变量全表）。关键配置包括：

- 数据库连接（主机、端口、用户名、密码）
- Redis 连接（主机、端口、密码）
- RabbitMQ 连接
- Elasticsearch 地址
- JWT 签名密钥（64 字符强随机字符串）
- OpenAI API 密钥（用于 Spring AI 对话/摘要/审核等功能）
- 内部 API 通信密钥（Python Agent 与后端通信使用）
- Nacos 配置（可选，用于服务发现和配置中心）

**2. 编译与启动**

使用 Maven 编译后端项目，以 Spring Boot 方式启动。服务端口为 8080，根路径为 `/api`。

启动后访问 http://localhost:8080/api/doc.html 查看 Swagger API 文档。

### 第三步：AI Agent 启动

**1. 配置环境变量**

进入 `project-ai-agent/` 目录，将 `.env.example` 复制为 `.env`，填写配置。关键配置包括：

- DeepSeek API 密钥（推荐用于 Agent 推理）
- OpenAI API 密钥（用于 Embedding 向量化）
- Tavily Search API 密钥（可选，用于外部互联网搜索）
- 数据库连接字符串（PostgreSQL + pgvector）
- Redis 连接字符串
- 后端通信密钥（必须与 Java 后端的 `API_UNIVERSAL_KEY` 一致）

**2. 安装依赖并启动**

创建 Python 虚拟环境，安装依赖，启动 FastAPI 服务。

启动后访问 http://localhost:8000/docs 查看 API 文档。

### 第四步：前端启动

**用户端：**

进入 `project-front/` 目录，安装依赖后启动开发服务器，默认端口 5173。

**管理端：**

进入 `project-front-admin/` 目录，安装依赖后启动开发服务器，默认端口 5174。

两个前端项目均无需额外配置文件，代理和 API 地址在 Vite 配置中已有默认值。

### 端口总览

| 服务 | 端口 |
|------|------|
| Spring Boot 后端 | 8080 |
| Python AI Agent | 8000 |
| 前端用户端 | 5173 |
| 前端管理端 | 5174 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Elasticsearch | 9200 |
| RabbitMQ | 5672 |
| Nacos | 8848 |

---

## 环境变量全表

所有敏感信息已从代码中移除，必须通过环境变量注入。本项目使用 `.env` 文件 + 环境变量双通道读取。

### Java 后端

配置位置：`project-backen/.env`

| 环境变量 | 说明 | 默认值 | 必要性 |
|---------|------|--------|--------|
| **DB_HOST** | PostgreSQL 主机地址 | `localhost` | 必需 |
| **DB_PORT** | PostgreSQL 端口 | `5432` | 必需 |
| **DB_NAME** | 数据库名称 | `person_blog` | 必需 |
| **DB_USERNAME** | 数据库用户名 | `postgres` | 必需 |
| **DB_PASSWORD** | 数据库密码 | — | 必需 |
| **REDIS_HOST** | Redis 主机地址 | `localhost` | 必需 |
| **REDIS_PORT** | Redis 端口 | `6379` | 必需 |
| **REDIS_PASSWORD** | Redis 密码 | — | 必需 |
| **REDIS_DATABASE** | Redis 数据库索引 | `2` | 必需 |
| **RABBITMQ_HOST** | RabbitMQ 主机地址 | `localhost` | 必需 |
| **RABBITMQ_PORT** | RabbitMQ 端口 | `5672` | 必需 |
| **RABBITMQ_USERNAME** | RabbitMQ 用户名 | `guest` | 必需 |
| **RABBITMQ_PASSWORD** | RabbitMQ 密码 | `guest` | 必需 |
| **ES_URIS** | Elasticsearch 地址 | `http://localhost:9200` | 必需 |
| **JWT_SECRET** | JWT 签名密钥（≥64 字符强随机字符串） | — | 必需 |
| **XXL_JOB_ADMIN_ADDRESSES** | XXL-Job 调度中心地址 | `http://localhost:8080/xxl-job-admin` | 可选 |
| **XXL_JOB_ACCESS_TOKEN** | XXL-Job 令牌 | — | 可选 |
| **XXL_JOB_EXECUTOR_IP** | 执行器 IP | — | 可选 |
| **XXL_JOB_EXECUTOR_PORT** | 执行器端口 | `9999` | 可选 |
| **NACOS_SERVER** | Nacos 服务器地址 | `localhost:8848` | 可选 |
| **NACOS_NAMESPACE** | Nacos 命名空间 ID | — | 可选 |
| **NACOS_GROUP** | Nacos 配置分组 | `DEFAULT_GROUP` | 可选 |
| **NACOS_USERNAME** | Nacos 用户名 | `nacos` | 可选 |
| **NACOS_PASSWORD** | Nacos 密码 | `nacos` | 可选 |
| **OSS_ACCESS_KEY_ID** | 阿里云 AccessKey ID | — | 可选 |
| **OSS_ACCESS_KEY_SECRET** | 阿里云 AccessKey Secret | — | 可选 |
| **ALIYUN_OSS_ENDPOINT** | OSS 地域节点 | `oss-cn-beijing.aliyuncs.com` | 可选 |
| **ALIYUN_OSS_BUCKET_NAME** | OSS 存储桶名称 | — | 可选 |
| **OPENAI_BASE_URL** | OpenAI API 基础 URL | `https://api.openai.com` | 必需 |
| **OPENAI_API_KEY** | OpenAI API 密钥 | — | 必需 |
| **OPENAI_MODEL** | 默认模型 | `gpt-4o-mini` | 必需 |
| **PYTHON_SERVICE_URL** | Python AI Agent 地址 | `http://127.0.0.1:8000` | 必需 |
| **API_UNIVERSAL_KEY** | 内部服务通信密钥 | — | 必需 |

### Python AI Agent

配置位置：`project-ai-agent/.env`

| 环境变量 | 说明 | 默认值 | 必要性 |
|---------|------|--------|--------|
| **OPENAI_API_KEY_DEEPSEEK** | DeepSeek API 密钥 | — | 必需 |
| **OPENAI_BASE_URL_DEEPSEEK** | DeepSeek API 地址 | `https://api.deepseek.com` | 必需 |
| **MODEL_NAME_DEEPSEEK** | DeepSeek 模型 | `deepseek-chat` | 必需 |
| **OPENAI_API_KEY** | OpenAI API 密钥 | — | 可选（Embedding） |
| **OPENAI_BASE_URL** | OpenAI API 地址 | `https://api.openai.com/v1` | 可选 |
| **EMBEDDING_MODEL** | Embedding 模型 | `text-embedding-3-small` | 可选 |
| **MODEL_NAME** | OpenAI 模型 | `gpt-4o-mini` | 可选 |
| **TAVILY_API_KEY** | Tavily 搜索密钥 | — | 可选（外部搜索） |
| **BACKEND_API_KEY** | 后端的 API 通信密钥 | — | 必需 |
| **DATABASE_URL** | PostgreSQL 连接字符串 | `postgresql://postgres:postgres@localhost:5432/person_blog` | 必需 |
| **REDIS_URL** | Redis 连接字符串 | `redis://localhost:6379/2` | 必需 |
| **ES_HOST** | Elasticsearch 地址 | `http://localhost:9200` | 必需 |
| **BACKEND_API_BASE** | Java 后端 API 地址 | `http://localhost:8080/api` | 必需 |

> ⚠️ **重要**：Python Agent 的 `BACKEND_API_KEY` 必须与 Java 后端的 `API_UNIVERSAL_KEY` 设置为相同值，否则服务间通信会因鉴权失败而中断。

---

## 部署方式

### Docker 部署

可以将后端和 AI Agent 分别构建为 Docker 镜像，通过环境变量注入配置。

**后端 Docker：**
将 Spring Boot 应用打包为 JAR 后，使用 JDK 21 基础镜像运行，通过 `-e` 参数注入环境变量。

**AI Agent Docker：**
使用 Python 3.12 基础镜像，安装依赖后运行，同样通过 `-e` 参数注入环境变量。

两个前端项目可构建为静态文件，使用 Nginx 或 Caddy 提供 HTTP 服务。

### 传统部署

**方式一——系统环境变量：**
在启动前通过 `export`（Linux/macOS）或系统设置配置所有环境变量，然后直接启动 JAR 或 Python 服务。

**方式二——启动参数：**
通过 `--spring.datasource.password=xxx` 等命令行参数直接传入 Spring Boot 配置。

**方式三——.env 文件（仅开发环境）：**
将 `.env.example` 复制为 `.env` 填写配置后启动。`.env` 文件已被 `.gitignore` 忽略，**切勿提交到 Git 仓库**。

---

## 安全建议

### 密钥管理

- **绝不提交 `.env` 文件到 Git**：`.gitignore` 已配置忽略，每次 commit 前请确认
- **不同环境使用不同密钥**：开发、测试、生产环境用不同的 JWT Secret、API Key 和数据库密码
- **强密钥生成**：JWT Secret 建议 64 字符以上，使用 `openssl rand -base64 48` 生成

### 密钥轮换建议

- JWT Secret：每 90 天轮换一次
- API Keys：每 180 天轮换一次
- 数据库密码：每 180 天轮换一次

### 访问控制

- 数据库：限制访问来源 IP
- Redis：启用密码认证，不暴露到公网
- RabbitMQ：使用非默认用户名密码
- 服务间 API Key：不要硬编码在任何前端代码中

---

## 项目结构

```
ByteBlog/
├── project-backen/           # Spring Boot 后端（12 个 Maven 子模块）
│   ├── blog-common/          # 公共基础设施（工具类、DTO、常量）
│   ├── blog-api/             # 模块间接口契约
│   ├── blog-push/            # 实时推送通道（WebSocket + SSE）
│   ├── blog-security/        # 认证授权
│   ├── blog-article/         # 文章管理
│   ├── blog-comment/         # 评论管理
│   ├── blog-interaction/     # 社交互动
│   ├── blog-question/        # 问答社区
│   ├── blog-search/          # 全文搜索
│   ├── blog-ai/              # AI 能力
│   ├── blog-job/             # 定时任务
│   └── blog-application/     # 启动模块
├── project-front/            # 前端用户端（Vue 3 + NaiveUI）
├── project-front-admin/      # 前端管理端（Vue 3 + Element Plus）
├── project-ai-agent/         # Python AI Agent（FastAPI + LangGraph）
├── sql/                      # 数据库 DDL 脚本
├── docs/                     # 项目文档
│   ├── AI模块文档/           # AI 模块设计/接口/流程文档
│   ├── 开发文档/             # 各功能开发文档
│   ├── 接口文档/             # API 接口文档
│   ├── 管理端接口文档/        # 管理端接口文档
│   ├── 认证安全/             # 鉴权体系文档
│   ├── 问题总结/             # 技术问题修复记录
│   ├── bug排查/              # Bug 排查记录
│   └── 未来规划/             # 开发规划
├── ENV_SETUP.md              # 环境变量配置详细指南
├── .gitignore                # Git 忽略规则
└── README.md                 # 本文件
```

---

## API 文档索引

### Spring Boot 后端 API

启动后访问 http://localhost:8080/api/doc.html 在线查看，或查阅 `docs/接口文档/`：

| 文档 | 内容 |
|------|------|
| 安全模块接口文档 | 登录/注册/Refresh Token/用户管理 |
| 文章模块接口文档 | 文章 CRUD、分类、标签 |
| 评论模块接口文档 | 评论增删查 |
| 互动模块接口文档 | 点赞、收藏、关注、私信、通知 |
| 问答模块接口文档 | 问答社区 |
| 搜索模块接口文档 | 全文搜索、搜索建议 |
| 管理端接口文档目录 | 认证/仪表盘/文章/分类/标签/评论/用户/专栏/问答/通知/日志管理 |

### Python AI Agent API

启动后访问 http://localhost:8000/docs 在线查看：

| 路由前缀 | 功能 |
|---------|------|
| `/api/v1/chat` | 智能对话（支持工具调用 + 深度思考 + SSE 流式） |
| `/api/v1/writing` | AI 写作（4 阶段工作流 + SSE 流式） |
| `/api/v1/rag` | RAG 知识库问答 |
| `/api/v1/knowledge` | 知识库文档上传/管理 |

