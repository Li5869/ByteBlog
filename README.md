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
  <img src="https://img.shields.io/badge/PostgreSQL-18-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/pgvector-0.82-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="pgvector">
  <img src="https://img.shields.io/badge/Elasticsearch-8.x-005571?style=flat-square&logo=elasticsearch&logoColor=white" alt="Elasticsearch">
  <img src="https://img.shields.io/badge/Nacos-2.x-00BFFF?style=flat-square&logo=nacos&logoColor=white" alt="Nacos">
</p>

</div>

---

## 项目简介

ByteBlog 是一个面向开发者的 **AI 增强全栈技术博客平台**，覆盖 **文章发布、问答社区、AI 智能写作与对话、全文检索、实时互动** 等核心场景。项目采用 **模块化单体架构**，后端 15 个 Maven 模块严格分层，通过 `blog-api` 契约接口层解决跨模块循环依赖；AI 侧采用 **Spring AI + Python Agent 双引擎架构**，Spring AI 处理简单同步任务（摘要、审核），Python LangChain/LangGraph 处理复杂异步工作流（对话、写作、RAG）。

技术选型对标工业级应用：JDK 21 虚拟线程压榨并发性能、Caffeine + Redis + DB 三级缓存对抗热点、Elasticsearch 毫秒级全文搜索、RabbitMQ 异步解耦削峰、WebSocket + SSE 双通道实时推送、LangGraph 智能 Agent 驱动 AI 写作与对话、Nacos 服务发现与配置中心实现动态配置管理。

---

## 核心特性

| 模块 | 能力 | 实现方式 |
|------|------|----------|
| 🤖 **AI 写作 Agent** | Plan-and-Execute 四阶段工作流（规划→执行→反思→定稿），四角色 LLM 差异化 temperature 配置，5 维质量评估自动微调，SSE 实时推送子步骤进度，Redis 任务状态管理支持断点恢复 | LangGraph StateGraph + DeepSeek |
| 💬 **AI 智能对话** | ReAct 范式循环推理（thinking 节点统一推理与回答 → judge 路由 → tool_executor 执行工具），DeepSeek 思考模式实时输出思维链，Parent-Child RAG 技术（pgvector 检索 Child Chunks → 聚合还原 Parent Documents），多工具并发调度（asyncio.gather），SSE 流式输出，add_messages reducer 状态管理，Skill 渐进式披露 + 向量化检索节省 Token + 三级降级策略 | LangGraph ReAct + pgvector |
| 📚 **RAG 知识库** | Parent-Child 文档切片策略（Child 450 字符 / Parent 1500 字符），OpenAI Embedding 向量化，pgvector 余弦相似度检索，管理端支持文档上传与管理 | OpenAI Embedding + pgvector |
| 🔍 **全文搜索** | ES 统一搜索（文章/问答/作者/专栏四类内容），BoolQuery + MultiMatch 多字段加权检索（title^2），Completion Suggester 搜索建议，MQ 增量同步 | Spring Data Elasticsearch 8.16 |
| ⚡ **三级缓存** | Caffeine（L1）→ Redis（L2）→ DB（L3）三级回源，Caffeine 原子加载防缓存击穿，NULL_VALUE 占位符防缓存穿透，CacheEntry 键级独立 TTL，Redis 故障自动降级 | Caffeine + Redis + Redisson |
| 📬 **消息解耦** | 点赞/评论/通知/ES 同步等写操作统一 MQ 异步处理；中央死信处理器统一重试（x-death 路由还原 + 3 次递增延迟），保障最终一致性；核心接口响应稳定在 50ms 以内 | RabbitMQ + 死信队列 |
| 💬 **实时通信** | WebSocket（私信/在线状态/心跳保活）+ SSE（点赞/评论/关注通知）双通道；Redisson Pub/Sub 跨节点消息分发；单用户多设备连接支持 | WebSocket + SseEmitter + Redisson |
| 🎨 **双前端** | 用户端 NaiveUI 社区风格（Vditor Markdown 编辑器）+ 管理端 Element Plus 后台风格（ECharts 仪表盘），Tailwind 响应式布局，json-bigint 解决雪花 ID 精度丢失 | Vue 3 + Vite + Pinia |
| 🔐 **安全认证** | JWT 双令牌（Access 30min + Refresh 7d），Redis 服务端状态管理支持可控登出，API Key 服务间鉴权，Nacos 动态 Prompt 配置 | Spring Security + JJWT |
| 🩺 **服务治理** | Nacos 服务发现 + 配置中心，AI Prompt 模板 @RefreshScope 动态刷新，Micrometer 业务指标监控（AI 调用/文章发布），对接 Prometheus | Nacos + Micrometer |

---

## 技术栈

### 后端 Java

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | 运行时 + 虚拟线程 |
| Spring Boot | 4.0.4 | 主框架 |
| Spring AI | 2.0.0-M4 | 内容审核 / 摘要生成 / 文章润色 |
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

---

## 功能截图

| 截图 | 内容 | 展示亮点 |
|------|------|----------|
| ![主页展示](D:\code\Java\agent-project\ByteBlog\docs\gif\主页展示.gif) | 首页文章列表 + 分类导航 + 热门标签 | 整体 UI 风格、响应式布局、Tailwind 设计 |
| ![文章详情](docs/gif/文章详情.gif) | 文章详情页（Markdown 渲染 + 评论区 + 点赞/收藏按钮） | 文章阅读体验、评论交互、社交互动 |
| ![AI 助手全流程写作](docs/gif/AI助手调用写作agent完成全流程写作功能.gif) | SmartAgent调用写作Agent全流程（用户输入需求 → SmartAgent理解意图 → 调用写作Agent → 生成计划 → 人工确认 → 开始写作 → 协作发布） | 多Agent协作、Plan-and-Execute 工作流、SSE 流式输出 |
| ![AI 润色和发布](docs/gif/ai润色和发布文章.gif) | 用户创作+AI润色（自己撰写内容，使用AI进行润色、生成标题和摘要） | Spring AI 集成、SSE 流式输出、人工创作与AI辅助结合 |
| ![AI 创作全流程](docs/gif/ai创作全流程.gif) | AI 写作Agent使用流程（从需求输入到文章生成的完整创作过程） | Plan-and-Execute 工作流、SSE 流式输出、进度实时推送 |
| ![文章评论和审核](docs/gif/文章评论和ai审核通知.gif) | 评论发布 + AI 审核通知 | AI 内容审核、SSE 实时通知推送 |
| ![私信和通知](docs/gif/私信功能和通知.gif) | 私信聊天 + 实时通知 | WebSocket 双向通信、SSE 通知推送 |
| ![AI 写作规划](docs/gif/ai写作规划.gif) | AI 写作计划生成与审批 | 人工审批、计划结构化展示 |
| ![RAG 知识库问答](docs/gif/RAG知识库相关问答.gif) | RAG 知识库问答界面（左侧文档列表 + 右侧问答交互） | Parent-Child RAG 技术、pgvector 语义检索、文档上传与管理 |
| ![ES 搜索提示和高亮](docs/gif/es搜索提示和高亮展示.gif) | ES 全文搜索（搜索提示 + 结果高亮展示） | BoolQuery + MultiMatch 多字段加权检索、Completion Suggester 搜索建议、关键词高亮 |

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
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │interact. │ │  search  │ │  job     │ │  admin   │ │  push    │  │
│  │ 社交互动  │ │ 全文搜索  │ │ 定时任务  │ │ 后台管理  │ │ 实时推送  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │notif.    │ │ message  │ │  common  │ │ blog-api │               │
│  │ 通知中心  │ │ 私信服务  │ │ 公共设施  │ │ 接口契约  │               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
│  ┌──────────┐                                                       │
│  │blog-app  │                                                       │
│  │ 启动模块  │                                                       │
│  └──────────┘                                                       │
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
│  │  Reflect→Finalize  │  │  多工具调用     │  │  语义检索→LLM 回答 │  │
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
project-backen/  —— Spring Boot 4 + Maven 多模块（15 个子模块）
│
├── blog-common/      # 公共基础设施
│   ├── constant/              常量定义（DlqConstants、MqRoutingConstants）
│   ├── dto/                   跨模块共享 DTO（FollowMessageDTO、BrowseHistoryMessageDTO 等）
│   ├── config/                基础配置（Redis、Redisson、MyBatis Plus、OSS）
│   ├── utils/                 工具类（RedisUtil、MultiLevelCacheUtil、UserContextHolder）
│   ├── exception/             统一异常处理
│   └── result/                统一响应封装
│
├── blog-api/         # 模块间接口契约层
│   ├── adminAPI/              TagApi、AdminLogApi（后台管理接口）
│   ├── interactionAPI/        LikeApi、FollowApi、BrowseHistoryApi（互动接口）
│   ├── messageAPI/            ConversationApi（私信接口）
│   ├── searchAPI/             SearchSyncApi（搜索同步接口）
│   └── ...                    其他业务接口定义
│
├── blog-admin/       # 后台管理模块 
│   ├── entity/                Tag、AdminLog、SensitiveWord、SystemConfig、MqErrorLog
│   ├── controller/            TagController、AdminLogController、AdminTagController
│   ├── service/               标签管理、操作日志、敏感词、系统配置服务
│   ├── aspect/                OperatorLogAspect（操作日志切面）
│   └── mqHandler/             DlqRetryHandler（统一死信队列重试处理器）
│
├── blog-notification/ # 通知中心模块 
│   ├── entity/                BizNotification、SystemNotification
│   ├── controller/            BizNotificationController、SystemNotificationController
│   ├── service/               业务通知、系统通知服务
│   └── mqHandler/             NotificationMqHandler（通知消息处理器）
│
├── blog-message/     # 私信服务模块 
│   ├── entity/                Conversation、Message
│   ├── controller/            MessageController
│   ├── service/               会话管理、消息管理服务
│   └── mapper/                ConversationMapper、MessageMapper
│
├── blog-push/        # 实时推送通道
│   ├── websocket/              WebSocket 连接管理 + 心跳
│   ├── sse/SseEmitterManager（单用户多设备 ConcurrentHashMap）
│   └── PushChannelService（Redisson Pub/Sub 跨节点分发）
│
├── blog-security/    # 认证授权
│   ├── JWT 双令牌（Access 30min + Refresh 7d）
│   ├── API Key 鉴权（服务间通信）
│   └── 方法级权限 @PreAuthorize
│
├── blog-article/     # 文章管理
│   ├── entity/                Article、Category、Column、ColumnArticle、ColumnSubscription
│   ├── controller/            文章 CRUD、分类管理、专栏管理
│   ├── config/mqConfig/       ArticleMqConfig、ArticleStatsMqConfig（MQ 配置已迁移）
│   └── mqHandler/             ArticleStatsMqHandler（文章统计处理器）
│
├── blog-comment/     # 评论管理
│   ├── 创建评论 → MQ（AI 审核队列 + 通知队列）
│   ├── config/mqConfig/       CommentMqConfig（MQ 配置已迁移）
│   └── mqHandler/             AICommentHandler、CommentNotificationHandler
│
├── blog-interaction/ # 社交互动（已精简）
│   ├── 点赞（Redis Set 原子操作 → Pipeline 批量查询 → MQ 落库）
│   ├── LikeStrategy 策略模式（文章/评论/问答/回答统一点赞行为）
│   ├── 关注/收藏/浏览历史（通知、私信已拆分到独立模块）
│   ├── config/mqConfig/       InteractionMqConfig（MQ 配置已迁移）
│   └── mqHandler/             BrowseHistoryMqHandler、CollectionMqHandler
│
├── blog-question/    # 问答社区（提问、回答、采纳）
│
├── blog-search/      # Elasticsearch 全文搜索
│   ├── BoolQuery + MultiMatch 多字段加权搜索（title^2）
│   ├── Completion Suggester 搜索建议
│   ├── config/mqConfig/       SearchMqConfig（MQ 配置已迁移）
│   └── MQ 增量同步
│
├── blog-ai/          # AI 能力集成
│   ├── PythonAgentChatService（WebClient + Reactor Flux SSE 流式）
│   ├── PythonWritingService（写作任务全生命周期管理）
│   ├── NacosPromptProperties（@RefreshScope 动态 Prompt）
│   ├── config/mqConfig/       AiMqConfig（MQ 配置已迁移）
│   └── mqHandler/             AiTitleMqHandler、AiModerateMqHandler
│
├── blog-job/         # XXL-Job 定时任务
│
└── blog-application/ # 启动模块（聚合所有子模块）
```

---

## AI Agent 架构

Python AI Agent 服务（FastAPI :8000）包含三个独立 Agent，共享底层基础设施层：

```
project-ai-agent/
│
├── main.py               # FastAPI 入口（SSE + CORS + Nacos 注册）
│
├── api/                   # 路由层
│   ├── chat_router.py     # 智能对话 SSE 流式接口
│   ├── writing_router.py  # 写作任务全生命周期接口
│   ├── rag_router.py      # RAG 知识库问答
│   └── knowledge_router.py# 知识库文档上传/管理
│
├── agents/                # LangGraph Agent
│   ├── smart_agent.py     # ReAct 循环推理 Agent
│   └── writing_agent.py   # Plan-and-Execute 写作 Agent
│
├── skills/                # Skills 渐进式披露
│   ├── loader.py           # SKILL.md 加载器
│   ├── smart-chat/         # 智能对话
│   ├── article-search/     # 文章搜索
│   ├── author-discovery/   # 博主探索
│   ├── knowledge-qa/       # 知识库问答
│   └── smart-search/       # 综合搜索
│
├── tools/                 # Agent 工具集
│   ├── article_tool.py      # ES 文章搜索
│   ├── vector_tool.py       # pgvector 知识库（Parent-Child RAG）
│   ├── author_tool.py       # 作者搜索
│   ├── blog_tool.py         # 分类/标签
│   ├── smart_search_tool.py # 站内→外部 二级降级搜索
│   ├── skill_tool.py        # Skill 详情披露工具
│   └── user_tool.py         # 用户上下文（contextvars 协程安全）
│
├── services/              # 三层服务架构
│   ├── core/                # 基础设施层（LLM/Embedding/Memory/Nacos）
│   ├── store/               # 数据存储层（ES/PostgreSQL/pgvector）
│   └── business/            # 业务逻辑层（写作内容/质量/标签/任务）
│
├── models/                # Pydantic 数据模型
├── config/prompts/        # 8 个 Prompt 模板集中管理
└── vectorstore/           # pgvector 向量存储封装
```

> **Skills 渐进式披露机制**：每个技能定义为独立 `SKILL.md` 文件（YAML frontmatter + Markdown 指南），系统提示词仅含技能名称与一句话描述，Agent 通过 `get_skill_details` 工具按需获取详情。解耦提示词与代码，支持横向扩展 10+ 技能。
>
> **向量化 Skill 节省 Token**：`search_skill_guide` 工具基于 pgvector 语义检索，只返回与当前任务相关的 Skill 指南片段，Token 消耗通常只有完整文档的 20-40%，支持跨 Skill 搜索。
>
> **Skill 三级降级策略**：① 向量检索成功 → 返回格式化切片；② 检索结果不足 → 返回切片 + 降级提示；③ 向量检索异常 → 自动降级加载完整文档，保障可用性。

---

### 写作 Agent 工作流

基于 **LangGraph StateGraph** 的 **Plan-and-Execute 模式**，采用官方推荐的 **Parallelization** 和 **Evaluator-Optimizer** 最佳实践，SSE 实时推送子步骤进度，Redis 管理任务状态支持断点恢复。

```
┌────────────────────────────────────────────────────────────────────┐
│  Writing Agent — Plan-and-Execute 四阶段工作流                     │
│                                                                    │
│  ┌──────────┐                                                      │
│  │  PLAN    │  LLM 分析需求 → 结构化计划（主题/风格/大纲/关键词）     │
│  │  规划    │  智能搜索参考资料（ES 优先 → Tavily 补充）             │
│  └────┬─────┘  SSE → plan_ready 事件，等待用户确认                  │
│       │                                                             │
│       ▼          LLM temperature = 0.1（内容策划师，精确可控）      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  EXECUTE 执行阶段（LangGraph 原生并行）                        │  │
│  │                                                              │  │
│  │  ┌─────────────┐    ┌─────────────┐                         │  │
│  │  │ Title Agent │    │ Tags Agent  │  ← 并行执行              │  │
│  │  │ 生成标题     │    │ 分类标签     │    asyncio.gather       │  │
│  │  └──────┬──────┘    └──────┬──────┘                         │  │
│  │         │                  │                                 │  │
│  │         └──────────┬───────┘                                 │  │
│  │                    │  Annotated[list, add] reducer          │  │
│  │                    │  自动合并并行输出到 parallel_outputs     │  │
│  │                    ▼                                         │  │
│  │             ┌─────────────┐                                  │  │
│  │             │ Merge Agent │  ← 合并标题+标签                  │  │
│  │             │   汇合点     │    allTagNames 包含所有标签      │  │
│  │             └──────┬──────┘                                  │  │
│  │                    │                                         │  │
│  │             ┌──────▼───────┐                                 │  │
│  │             │ Summary Agent│  ← 生成摘要                      │  │
│  │             └──────┬───────┘                                 │  │
│  │                    │                                         │  │
│  │             ┌──────▼───────┐                                 │  │
│  │             │ Content Agent│  ← 生成正文                      │  │
│  │             └──────┬───────┘                                 │  │
│  └───────────────────┼──────────────────────────────────────────┘  │
│                      │                                             │
│                      ▼          LLM temperature = 0.6（技术博客）  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  REFLECT 反思阶段（Evaluator-Optimizer 循环）                 │  │
│  │                                                              │  │
│  │  ┌─────────────┐                                             │  │
│  │  │ Evaluate    │  ← 5 维评分：完整性(30%) + 结构性(20%)      │  │
│  │  │   Agent     │              + 表达(25%) + 实用性(15%)      │  │
│  │  └──────┬──────┘              + 格式(10%)                    │  │
│  │         │                                                     │  │
│  │         │  评分 < 7.0 且修订次数 < 3                          │  │
│  │         │                                                     │  │
│  │         ▼                                                     │  │
│  │  ┌─────────────┐                                             │  │
│  │  │ Revise      │  ← 精细化修订（针对性修改）                  │  │
│  │  │   Agent     │                                             │  │
│  │  └──────┬──────┘                                             │  │
│  │         │                                                     │  │
│  │         └──→ 循环回 Evaluate（最多 3 次）                     │  │
│  │                                                              │  │
│  │  评分达标或修订耗尽 → 进入 Finalize                           │  │
│  └───────────────────┼──────────────────────────────────────────┘  │
│                      │                                             │
│                      ▼          LLM temperature = 0.1（审稿人）    │
│  ┌──────────┐                                                      │
│  │ FINALIZE │  保存完整内容到 Java 后端（含分类/标签 ID）           │
│  │  定稿    │  SSE → finalize_ready   │
│  └──────────┘                                                      │
└────────────────────────────────────────────────────────────────────┘
```

**架构亮点（基于 LangGraph 官方最佳实践）：**

| 特性 | 实现方式 | 官方文档参考 |
|------|---------|-------------|
| **原生并行模式** | `Annotated[list, operator.add]` reducer 自动合并并行输出 | [Parallelization](https://docs.langchain.com/oss/python/langgraph/workflows-agents#parallelization) |
| **Evaluator-Optimizer 循环** | evaluate → revise → evaluate 循环，精细化修订 | [Evaluator-optimizer](https://docs.langchain.com/oss/python/langgraph/workflows-agents#evaluator-optimizer) |
| **节点单一职责** | 每个节点只做一件事，State 存储原始数据 | [Thinking in LangGraph](https://docs.langchain.com/oss/python/langgraph/thinking-in-langgraph) |
| **SSE 流式推送** | 实时推送各阶段进度，前端实时更新 | [Event streaming](https://docs.langchain.com/oss/python/langgraph/event-streaming) |
| **Human-in-the-loop** | `interrupt_before` 等待用户确认计划 | [Persistence](https://docs.langchain.com/oss/python/langgraph/persistence) |

![AI 写作工作流](docs/gif/ai写作规划.gif)
> *用户输入写作需求 → 生成计划 → 确认 → 流式输出标题/摘要/正文 → 质量评估结果的完整流程*

![SmartAgent调用写作Agent全流程](docs/gif/AI助手调用写作agent完成全流程写作功能.gif)
> *SmartAgent调用写作Agent全流程，展示用户输入需求 → SmartAgent理解意图 → 调用写作Agent → 生成计划 → 人工确认 → 开始写作 → 协作发布的完整过程*

---

### Smart Agent 工具调用

基于 **LangGraph StateGraph** 的 **Thinking→Judge→ToolExecutor** ReAct 循环图，thinking 节点统一负责推理与回答输出，judge 根据 `tool_calls` 路由至工具执行或结束。

**工作流：**

```
thinking ──→ judge ──→ (有 tool_calls) ──→ tool_executor ──→ thinking (循环)
    │
    └──→ (无 tool_calls) ──→ END
```

**核心机制：**

- **DeepSeek 官方思考模式**：`reasoning_content`（思维链）→ `thinking` 事件，`content`（最终回答）→ `chunk` 事件，天然分离无需额外解析
- **LangGraph 原生 custom 流模式**：thinking 节点通过 `get_stream_writer()` 实时发射 thinking / chunk / tool_call 事件，零缓存回放
- **add_messages reducer**：节点只需返回新消息，LangGraph 自动追加到消息历史，代码简洁且不易出错
- **并发工具调用**：`asyncio.gather` 同时执行多个工具调用
- **迭代次数控制**：超过最大迭代次数强制结束，防止无限循环
- **content + tool_calls 并发输出**：模型可先回答部分内容（如"让我查一下数据库"），同时发起工具调用，下轮基于工具结果继续完善回答

**工具列表（22 个）：**

| 工具 | 功能 | 来源 |
|------|------|------|
| `search_articles_by_keyword` | ES 关键词搜索文章 | Elasticsearch |
| `search_knowledge_base` | Parent-Child RAG 语义检索（支持 category 过滤） | pgvector |
| `search_authors_by_keyword` | ES 博主搜索（用户名/昵称/简介） | Elasticsearch |
| `get_hot_articles` | 获取浏览量最高的热门文章 | Elasticsearch |
| `get_hot_authors` | 获取粉丝数最多的热门博主 | Elasticsearch |
| `get_article_by_id` | 根据 ID 获取文章详情 | Elasticsearch |
| `get_article_content_by_id` | 根据 ID 获取文章完整 Markdown 正文 | Java API |
| `get_author_by_id` | 根据 ID 获取博主详细信息 | Elasticsearch |
| `get_category_list` | 获取所有文章分类 | Java API |
| `get_the_time` | 获取当前日期时间、星期、时间段描述 | 系统时钟 |
| `get_current_user_id` | 获取当前登录用户 ID | 用户上下文 |
| `get_current_user_info` | 获取当前登录用户详细信息（昵称/邮箱/头像等） | 用户上下文 |
| `get_skill_details` | 获取 Skill 详细使用指南（渐进式披露） | Skills 文件 |
| `list_available_skills` | 列出所有可用 Skills 及简要描述 | Skills 文件 |
| `search_skill_guide` | 语义搜索 Skill 指南片段（低 Token 消耗） | Skills 文件 |
| `search_external_tech_blogs` | 站外技术博客搜索（站内结果不足时补充） | Tavily |
| `scrape_webpage` | 爬取单个网页内容，提取正文转 Markdown | Web 爬虫 |
| `scrape_multiple_webpages` | 并发爬取多个网页（最多 5 个） | Web 爬虫 |
| `writing_start` | 启动写作任务，异步生成计划 | 写作 Agent |
| `writing_status` | 查询写作任务状态及计划内容 | 写作 Agent |
| `writing_action` | 执行写作动作（确认/修订/取消） | 写作 Agent |
| `writing_result` | 获取写作成果链接 | 写作 Agent |
| `writing_publish` | 发布或保存草稿 | 写作 Agent |

![SmartAgent调用写作Agent全流程](docs/gif/ai创作全流程.gif)
> *SmartAgent调用写作Agent全流程，展示用户输入需求 → SmartAgent理解意图 → 调用写作Agent → 生成计划 → 人工确认 → 开始写作 → 协作发布的完整过程*

---

### RAG 知识库问答 — Parent-Child 策略

```
管理端上传文档 (PDF/MD/TXT)
        │
        ▼
┌─────────────────────────────────────┐
│  文档切片                             │
│  ┌────────────────────────────────┐  │
│  │ Parent Chunk (1500 字符)       │  │
│  │  ├─ Child Chunk (450 字符)     │  │
│  │  ├─ Child Chunk (450 字符)     │  │
│  │  └─ 重叠 50 字符               │  │
│  └────────────────────────────────┘  │
│  RecursiveCharacterTextSplitter      │
└──────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────┐
│  向量化存储（OpenAI Embedding → pgvector）│
└──────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────┐
│  用户提问 → 语义检索 Top-K Child     │
│         → 提取 doc_id（去重）        │
│         → 批量获取完整 Parent Doc    │
│         → LLM 综合回答              │
└──────────────────────────────────────┘
```

![RAG 知识库问答界面](docs/gif/RAG知识库相关问答.gif)
> *左侧知识库文档列表 + 右侧问答交互，展示上传文档后基于文档内容提问的效果*

---

## 全文搜索

ByteBlog 采用 **Elasticsearch** 实现毫秒级全文搜索，支持文章/问答/作者/专栏四类内容的统一检索。

**核心能力：**

| 功能 | 说明 |
|------|------|
| **BoolQuery + MultiMatch** | 多字段加权检索（title^2），精准匹配标题与内容 |
| **Completion Suggester** | 搜索建议，输入即提示，提升搜索体验 |
| **MQ 增量同步** | 文章发布/更新时通过 RabbitMQ 异步同步到 ES，保障数据一致性 |
| **关键词高亮** | 搜索结果中关键词高亮展示，快速定位相关内容 |

![ES 搜索提示和高亮展示](docs/gif/es搜索提示和高亮展示.gif)
> *搜索框输入关键词时的搜索建议、搜索结果的关键词高亮展示*

---

## 实时通信体系

ByteBlog 采用 **WebSocket + SSE 双通道**，由独立的 `blog-push` 模块承载，并通过 **Redisson Pub/Sub** 实现多实例部署下的跨节点消息分发。

| 通道 | 用途 | 方向 | 路径 |
|------|------|------|------|
| **WebSocket** | 私信推送、在线状态、心跳检测 | 双向 | `ws://host/ws` |
| **SSE** | 点赞/评论/关注/系统通知推送 | 服务端→客户端 | `GET /interaction/sse/connect` |
| **Redisson Pub/Sub** | 多实例跨节点消息分发 | 服务端→服务端 | Topic: `push:channel` |

**WebSocket 整体流程：**
```
客户端 → JWT 鉴权 → 建立 Session → 踢掉旧连接 → 标记在线 → 心跳保活 (15s)
                                                              │
                                                              ▼
                        发送私信 → 检查接收方在线 → WebSocket 推送 → 前端弹窗
```

**SSE 通知流程：**
```
产生通知（点赞/评论/关注） → MQ 异步处理 → Redisson Pub/Sub 跨节点分发
                                            → SseEmitter 推送 → 前端弹窗
```

**连接管理：**
- `ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>` 支持单用户多设备
- `isClientDisconnect` 智能检测断连，递归查找 IOException 异常链
- `onCompletion` / `onTimeout` / `onError` 回调自动清理

![实时通知截图](docs/gif/文章评论和ai审核通知.gif)
> *收到点赞/评论通知时前端的弹窗效果，展示 SSE 通知推送*

![私信聊天界面](docs/gif/私信功能和通知.gif)
> *私信聊天窗口，展示 WebSocket 实时消息推送效果*

---

## 持久层策略

| 存储 | 用途 | 策略 |
|------|------|------|
| **PostgreSQL** | 业务数据持久化 | 雪花 ID 主键（@JsonSerialize ToStringSerializer 防前端精度丢失），MyBatis Plus ORM |
| **pgvector** | 向量数据 | 同一 PostgreSQL 实例 + vector 扩展，Parent-Child RAG，余弦相似度搜索 |
| **Caffeine（L1）** | 本地进程内缓存 | 原子 get 加载防击穿，CacheEntry 键级 TTL，各模块独立实例（容量 10~500，TTL 2min~1h） |
| **Redis（L2）** | 分布式缓存 + 计数器 + 在线状态 + 分布式锁 | NULL_VALUE 占位符防穿透，GenericJacksonJsonRedisSerializer 序列化 |
| **RabbitMQ** | 异步解耦 | 16 个队列 + 对应死信队列，统一 DlqRetryHandler 重试，手动 ACK |
| **Elasticsearch** | 全文搜索 | 4 类文档索引（文章/问答/作者/专栏），BoolQuery + MultiMatch，MQ 增量同步 |
| **Nacos** | 服务发现 + 配置中心 | 服务注册发现，@RefreshScope 动态配置，AI Prompt 集中管理 |

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

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL 18 | 5432 | 业务数据库，需启用 pgvector 扩展 |
| Redis 7.4.7 | 6379 | 缓存 / 分布式锁 / 在线状态 |
| Elasticsearch 8.x | 9200 | 全文检索 |
| RabbitMQ 3.x | 5672 | 异步消息 |
| Nacos 2.x | 8848 | 服务发现 + 配置中心（可选） |

**数据库初始化：**
```bash
# 创建数据库并启用 pgvector 扩展
createdb person_blog
psql -d person_blog -c "CREATE EXTENSION vector;"
# 导入建表脚本
psql -d person_blog -f sql/public.sql
```

### 第二步：后端启动

```bash
cd project-backen/
# 复制并配置环境变量
cp .env.example .env
# 编辑 .env 填写数据库/Redis/RabbitMQ/ES/JWT等配置
# 编译启动
mvn clean package -DskipTests
java -jar blog-application/target/blog-application-0.0.1-SNAPSHOT.jar
```

启动后访问 http://localhost:8080/api/doc.html 查看 API 文档。

### 第三步：AI Agent 启动

```bash
cd project-ai-agent/
# 创建虚拟环境
python -m venv .venv
.venv\Scripts\activate  # Windows
# 安装依赖
uv sync  # 或 pip install -r requirements.txt
# 配置环境变量
cp .env.example .env
# 启动服务
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

启动后访问 http://localhost:8000/docs 查看 API 文档。

### 第四步：前端启动

```bash
# 用户端
cd project-front/
npm install
npm run dev

# 管理端（新开终端）
cd project-front-admin/
npm install
npm run dev
```

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

### Java 后端（`project-backen/.env`）

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
| **RABBITMQ_HOST** | RabbitMQ 主机地址 | `localhost` | 必需 |
| **RABBITMQ_PORT** | RabbitMQ 端口 | `5672` | 必需 |
| **RABBITMQ_USERNAME** | RabbitMQ 用户名 | `guest` | 必需 |
| **RABBITMQ_PASSWORD** | RabbitMQ 密码 | `guest` | 必需 |
| **ES_URIS** | Elasticsearch 地址 | `http://localhost:9200` | 必需 |
| **JWT_SECRET** | JWT 签名密钥（≥64 字符强随机字符串） | — | 必需 |
| **OPENAI_BASE_URL** | OpenAI API 基础 URL | `https://api.openai.com` | 必需 |
| **OPENAI_API_KEY** | OpenAI API 密钥 | — | 必需 |
| **PYTHON_SERVICE_URL** | Python AI Agent 地址 | `http://127.0.0.1:8000` | 必需 |
| **API_UNIVERSAL_KEY** | 内部服务通信密钥 | — | 必需 |
| **NACOS_SERVER** | Nacos 服务器地址 | `localhost:8848` | 可选 |
| **XXL_JOB_ADMIN_ADDRESSES** | XXL-Job 调度中心地址 | `http://localhost:8080/xxl-job-admin` | 可选 |
| **OSS_ACCESS_KEY_ID** | 阿里云 OSS AccessKey | — | 可选 |

### Python AI Agent（`project-ai-agent/.env`）

| 环境变量 | 说明 | 默认值 | 必要性 |
|---------|------|--------|--------|
| **OPENAI_API_KEY_DEEPSEEK** | DeepSeek API 密钥 | — | 必需 |
| **OPENAI_BASE_URL_DEEPSEEK** | DeepSeek API 地址 | `https://api.deepseek.com` | 必需 |
| **OPENAI_API_KEY** | OpenAI API 密钥（Embedding） | — | 可选 |
| **TAVILY_API_KEY** | Tavily 搜索密钥 | — | 可选 |
| **BACKEND_API_KEY** | 后端 API 通信密钥 | — | 必需 |
| **DATABASE_URL** | PostgreSQL 连接字符串 | `postgresql://postgres:postgres@localhost:5432/person_blog` | 必需 |
| **REDIS_URL** | Redis 连接字符串 | `redis://localhost:6379/2` | 必需 |
| **ES_HOST** | Elasticsearch 地址 | `http://localhost:9200` | 必需 |
| **BACKEND_API_BASE** | Java 后端 API 地址 | `http://localhost:8080/api` | 必需 |

> ⚠️ **重要**：Python Agent 的 `BACKEND_API_KEY` 必须与 Java 后端的 `API_UNIVERSAL_KEY` 设置为相同值。

---

## 部署方式

### Docker 部署

```bash
# 后端 Docker
cd project-backen
mvn clean package -DskipTests
docker build -t byteblog-backend .

# AI Agent Docker
cd project-ai-agent
docker build -t byteblog-ai-agent .

# 前端构建为静态文件，使用 Nginx 提供服务
cd project-front && npm run build
cd project-front-admin && npm run build
```

### 传统部署

配置环境变量后直接启动：
```bash
# 后端
java -jar blog-application.jar

# AI Agent
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

---

## 安全建议

- **绝不提交 `.env` 文件到 Git**：`.gitignore` 已配置忽略
- **密钥轮换**：JWT Secret 每 90 天、API Keys 每 180 天、数据库密码每 180 天轮换一次
- **访问控制**：数据库限制来源 IP，Redis 启用密码认证且不暴露公网，RabbitMQ 使用非默认密码
- **服务间 API Key**：不要硬编码在前端代码中

---

## 项目结构

```
ByteBlog/
├── project-backen/           # Spring Boot 后端（15 个 Maven 子模块）
├── project-front/            # 前端用户端（Vue 3 + NaiveUI + Tailwind）
├── project-front-admin/      # 前端管理端（Vue 3 + Element Plus）
├── project-ai-agent/         # Python AI Agent（FastAPI + LangGraph）
├── sql/                      # 数据库 DDL 脚本
├── docker/                   # Docker 配置（Prometheus + Grafana 监控栈）
├── docs/                     # 项目文档
│   ├── v1.0-release-notes.md # v1.0 发布说明与收尾文档
│   ├── knowledge-base.md     # 项目知识库
│   ├── gif/                  # 功能演示 GIF
│   └── ...                   # 其他设计文档、方案文档
├── .gitignore                # Git 忽略规则
├── CLAUDE.md                 # AI 辅助开发规则
└── README.md                 # 本文件
```

---

## API 文档索引

### Spring Boot 后端 API

启动后访问 http://localhost:8080/api/doc.html 在线查看。

| 文档 | 内容 |
|------|------|
| 安全模块接口文档 | 登录/注册/Refresh Token/用户管理 |
| 文章模块接口文档 | 文章 CRUD、分类、标签 |
| 评论模块接口文档 | 评论增删查、审核 |
| 互动模块接口文档 | 点赞、收藏、关注、私信、通知 |
| 问答模块接口文档 | 问答社区 |
| 搜索模块接口文档 | 全文搜索、搜索建议 |
| 管理端接口文档目录 | 认证/仪表盘/文章/分类/标签/评论/用户/专栏/问答/通知/日志管理 |

### Python AI Agent API

启动后访问 http://localhost:8000/docs 在线查看。

| 路由前缀 | 功能 |
|---------|------|
| `/api/v1/chat` | 智能对话（SSE 流式 + 工具调用 + 深度思考） |
| `/api/v1/writing` | AI 写作（4 阶段工作流 + SSE 流式进度） |
| `/api/v1/rag` | RAG 知识库问答 |
| `/api/v1/knowledge` | 知识库文档上传/管理 |
