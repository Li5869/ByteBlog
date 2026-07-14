<div align="center">

# ByteBlog

**面向开发者的 AI 增强技术交流社区平台**

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.4-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Vue_3-3.5.13-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue 3">
  <img src="https://img.shields.io/badge/Python-3.12+-3776AB?style=flat-square&logo=python&logoColor=white" alt="Python">
  <img src="https://img.shields.io/badge/LangGraph-1.1+-1C3C3C?style=flat-square&logo=langchain&logoColor=white" alt="LangGraph">
</p>
<p align="center">
  <img src="https://img.shields.io/badge/PostgreSQL-18-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-7.2+-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/RabbitMQ-3.x-FF6600?style=flat-square&logo=rabbitmq&logoColor=white" alt="RabbitMQ">
  <img src="https://img.shields.io/badge/Elasticsearch-8.x-005571?style=flat-square&logo=elasticsearch&logoColor=white" alt="Elasticsearch">
</p>

</div>

---

## 项目简介

ByteBlog 是一个面向开发者的 **AI 增强技术交流社区平台**，覆盖 **文章发布、积分系统、VIP会员、优惠券、AI 智能写作与对话、三层记忆系统、深度研究Agent、RAG知识库、全文检索、实时互动** 等核心场景。项目采用 **模块化单体架构**，后端 17 个 Maven 模块严格分层，通过 `blog-api` 契约接口层解决跨模块循环依赖；AI 侧采用 **Spring AI + Python Agent 双引擎架构**，Spring AI 处理轻量同步任务（对话标题生成、文章标题润色、摘要提取、内容审核），Python LangChain/LangGraph 处理复杂异步工作流（对话、写作、RAG、深度研究）。

技术选型对标工业级应用：JDK 21 虚拟线程压榨并发性能、Caffeine + Redis + DB 三级缓存对抗热点、Elasticsearch 毫秒级全文搜索、RabbitMQ 异步解耦削峰、WebSocket + SSE 双通道实时推送、LangGraph 智能 Agent 驱动 AI 写作与对话、Nacos 服务发现与配置中心实现动态配置管理。

---

## 核心特性

| 模块 | 能力 | 实现方式 |
|------|------|----------|
| 🤖 **AI 写作 Agent** | 固定工作流四阶段（规划→执行→反思→定稿），四角色 LLM 差异化 temperature 配置，5 维质量评估自动微调，SSE 实时推送子步骤进度，Redis 任务状态管理支持断点恢复 | LangGraph StateGraph |
| 🔬 **AI 深度研究 Agent** | Orchestrator-Worker 模式（规划→并行执行→评估→报告），Send API 动态创建并行 Worker，Replanner 智能评估决定继续或生成报告，interrupt 人工审批计划，硬性终止条件防止无限循环 | LangGraph Orchestrator-Worker |
| 💬 **AI 智能对话** | Supervisor + Sub-Agent 多 Agent 架构（SmartAgent 调度 SearchAgent / KnowledgeAgent / WritingAgent / CodeExecutionAgent），记忆召回节点（memory_recall_node）首轮自动注入用户记忆，ReAct 范式循环推理，LLM 思考模式实时输出思维链，Parent-Child RAG 技术（pgvector 检索 Child Chunks → 聚合还原 Parent Documents），Sub-Agent 基于 create_agent 预构建 ReAct 循环，SSE 流式输出 | LangGraph ReAct + create_agent + pgvector |
| 🔧 **Skill 系统** | 渐进式披露设计（list_skills → get_skill_details → search_skill_guide 三级引导），pgvector 向量化检索节省 Token，三级降级策略（向量检索失败 → 关键词匹配 → 全量扫描），Skills 文件热加载支持动态扩展 | pgvector + LangChain |
| 🧠 **三层记忆系统** | 短期记忆——LangGraph MemorySaver 检查点持久化完整消息历史；中期记忆——LangMem summarize_messages 增量压缩（200K token 阈值触发，RunningSummary 避免重复压缩，独立 DeepSeek v4 flash 模型），超长对话自动摘要注入上下文；长期记忆——Mem0 记忆引擎（自托管 pgvector），CoALA 三类记忆模型（语义/情节/程序），双重召回策略（首轮自动召回 + recall_memory 工具按需召回），双重存储策略（save_memory 工具主动存储 + XXL-Job 后台定时提取），Redis 活跃标记 + Java XXL-Job 跨服务调度，Mem0 自动去重 + 时序推理 | LangGraph Checkpointer + LangMem + Mem0 + XXL-Job |
| 🖥️ **代码执行** | CodeExecutionAgent（Judge0 CE 沙箱），支持 60+ 编程语言，Supervisor 按需调度，沙箱隔离安全执行 | Judge0 CE + httpx |
| 📚 **RAG 知识库** | Parent-Child 文档切片策略（Child 450 字符 / Parent 1500 字符），OpenAI Embedding 向量化，pgvector 余弦相似度检索，category 分类隔离（project/interview/general 元数据过滤），管理端支持文档上传与管理 | OpenAI Embedding + pgvector |
| 🎯 **积分系统** | 用户签到、积分记录、排行榜、积分发放（文章发布/点赞/收藏等场景），MQ 异步处理保障最终一致性 | Redis + RabbitMQ |
| 🎫 **优惠券系统** | 高并发限时限量领券，Redis Lua 脚本原子操作（SISMEMBER 去重 + DECR 扣库存 + SADD 标记），本地消息表保证 MQ 可靠投递，消费端三层防超卖（幂等 + DB WHERE stock>0 + 回补 Redis） | Redis Lua + RabbitMQ + 本地消息表 |
| 👑 **VIP 会员** | TCC 分布式事务（Try 冻结 → Confirm 提交 → Cancel 回滚），积分冻结与券核销跨模块协调，分支状态机解决空回滚与悬挂，Confirm 部分失败逆序补偿，Redisson 分布式锁 + RabbitMQ 延迟队列超时自动取消 | TCC + Redisson + RabbitMQ |
| 🔍 **全文搜索** | ES 统一搜索（文章/作者/专栏三类内容），BoolQuery + MultiMatch 多字段加权检索（title^2），Completion Suggester 搜索建议，MQ 增量同步 | Spring Data Elasticsearch 8.16 |
| ⚡ **三级缓存** | Caffeine（L1）→ Redis（L2）→ DB（L3）三级回源，Caffeine 原子加载防缓存击穿，NULL_VALUE 占位符防缓存穿透，CacheEntry 键级独立 TTL，Redis 故障自动降级 | Caffeine + Redis + Redisson |
| 📨 **本地消息表** | 业务操作与消息写入同一事务保证原子性，trySend 尝试立即发送 MQ，失败后 XXL-Job 定时补偿，指数退避策略（2^n 秒），超过最大重试标记失败人工介入 | XXL-Job + RabbitMQ |
| 📬 **消息解耦** | 点赞/评论/通知/ES 同步等写操作统一 MQ 异步处理；中央死信处理器统一重试（x-death 路由还原 + 3 次递增延迟），保障最终一致性；核心接口响应稳定在 50ms 以内 | RabbitMQ + 死信队列 |
| 💬 **实时通信** | WebSocket（私信/在线状态/心跳保活）+ SSE（点赞/评论/关注通知）双通道；Redisson Pub/Sub 跨节点消息分发；单用户多设备连接支持 | WebSocket + SseEmitter + Redisson |
| 🎨 **双前端** | 用户端 NaiveUI 社区风格（Vditor Markdown 编辑器）+ 管理端 Element Plus 后台风格（ECharts 仪表盘），Tailwind 响应式布局，json-bigint 解决雪花 ID 精度丢失 | Vue 3 + Vite + Pinia |
| 🔐 **安全认证** | JWT 双令牌（Access 30min + Refresh 7d），Redis 服务端状态管理支持可控登出，API Key 服务间鉴权，Nacos 动态 Prompt 配置 | Spring Security + JJWT |
| 🩺 **服务治理** | Nacos 服务发现 + 配置中心，AI Prompt 模板 @RefreshScope 动态刷新，Micrometer 业务指标监控（AI 调用/文章发布），对接 Prometheus | Nacos + Micrometer |
| 📊 **LangSmith 可观测性** | Agent 调用链追踪（Trace）、LLM 请求/响应记录、工具调用耗时统计、Token 消耗分析，支持调试和性能优化 | LangSmith |

---

## 技术栈

### 后端 Java

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | 运行时 + 虚拟线程 |
| Spring Boot | 4.0.4 | 主框架 |
| Spring AI | 2.0.0-M4 | 对话标题生成 / 文章标题润色 / 摘要提取 / 内容审核 |
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
| LangGraph | ≥1.1.0 | Agent 有向图工作流 |
| LangChain-OpenAI | ≥1.2.0 | LLM 接入（兼容 OpenAI 协议） |
| LangChain-Postgres | ≥0.0.17 | pgvector 检查点存储 |
| LangChain-Tavily | ≥0.2.0 | 外部搜索工具 |
| OpenAI SDK | ≥1.60.0 | 原生客户端 |
| Elasticsearch | ≥8.16.0 | ES 搜索客户端 |
| pgvector | ≥0.2.5, <0.4 | 向量数据库 |
| Redis | ≥5.2.0 | 对话记忆 / 任务状态 |
| psycopg | ≥3.2.0 | PostgreSQL 驱动 |
| mem0ai | ≥2.0.0 | 用户长期记忆引擎（Mem0 AsyncMemory） |
| langmem | ≥0.0.30 | 中期记忆（对话压缩 summarize_messages） |
| langchain-deepseek | ≥1.0.1 | DeepSeek LLM 接入 |
| firecrawl-py | ≥1.0.0 | Firecrawl 网页爬取 |
| httpx | ≥0.28.0 | HTTP 异步客户端（含 Judge0 调用） |
| uvicorn | ≥0.34.0 | ASGI 服务器 |
| langsmith | ≥0.1.0 | LangSmith 可观测性（Agent 调用追踪） |
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

## 功能概览

| 截图 | 内容 | 展示亮点 |
|------|------|----------|
| ![主页展示](docs/演示素材/主页展示.gif) | 首页文章列表 + 分类导航 + 热门标签 | 整体 UI 风格、响应式布局、Tailwind 设计 |
| ![文章详情](docs/演示素材/文章详情.gif) | 文章详情页（Markdown 渲染 + 评论区 + 点赞/收藏按钮） | 文章阅读体验、评论交互、社交互动 |
| ![AI 助手全流程写作](docs/演示素材/AI助手调用写作agent完成全流程写作功能.gif) | SmartAgent调用写作Agent全流程（用户输入需求 → SmartAgent理解意图 → 调用写作Agent → 生成计划 → 人工确认 → 开始写作 → 协作发布） | 多Agent协作、固定工作流架构、SSE 流式输出 |
| ![AI 润色和发布](docs/演示素材/ai润色和发布文章.gif) | 用户创作+AI润色（自己撰写内容，使用AI进行润色、生成标题和摘要） | Spring AI 集成、SSE 流式输出、人工创作与AI辅助结合 |
| ![AI 创作全流程](docs/演示素材/ai创作全流程.gif) | AI 写作Agent使用流程（从需求输入到文章生成的完整创作过程） | 固定工作流架构、SSE 流式输出、进度实时推送 |
| ![文章评论和审核](docs/演示素材/文章评论和ai审核通知.gif) | 评论发布 + AI 审核通知 | AI 内容审核、SSE 实时通知推送 |
| ![私信和通知](docs/演示素材/私信功能和通知.gif) | 私信聊天 + 实时通知 | WebSocket 双向通信、SSE 通知推送 |
| ![AI 写作规划](docs/演示素材/ai写作规划.gif) | AI 写作计划生成与审批 | 人工审批、计划结构化展示 |
| ![RAG 知识库问答](docs/演示素材/RAG知识库相关问答.gif) | RAG 知识库问答界面（通过SmartAgent调用知识库子agent实现） | Parent-Child RAG 技术、pgvector 语义检索、文档上传与管理 |
| ![ES 搜索提示和高亮](docs/演示素材/es搜索提示和高亮展示.gif) | ES 全文搜索（搜索提示 + 结果高亮展示） | BoolQuery + MultiMatch 多字段加权检索、Completion Suggester 搜索建议、关键词高亮 |
| ![AI 智能对话](docs/演示素材/ai智能对话.gif) | AI 智能对话界面（多轮对话 + 工具调用 + 思维链展示） | Supervisor + Sub-Agent 多Agent协作、LLM 思考模式、记忆召回 |
| ![深度研究](docs/演示素材/深度研究.gif) | AI 深度研究功能（输入研究主题 → 生成计划 → 并行执行 → 生成报告） | Orchestrator-Worker 架构、Send API 动态并行、SSE 流式进度 |
| ![代码执行](docs/演示素材/代码执行.gif) | AI 代码执行功能（编写代码 → 沙箱执行 → 查看结果） | Judge0 CE 沙箱、60+ 编程语言支持、安全隔离执行 |
| ![个人中心](docs/演示素材/个人中心.gif) | 个人中心（积分记录 + 签到 + VIP 会员状态 + 收藏/关注） | 积分系统、用户画像、签到 |
| ![优惠券领取](docs/演示素材/优惠券领取.gif) | 优惠券中心（限时限量领券 + 我的优惠券） | Redis Lua 原子扣减、三层防超卖、本地消息表可靠投递 |
| ![会员购买](docs/演示素材/会员购买.gif) | VIP 会员购买（套餐选择 → 下单支付 → 权益解锁） | 本地事务原子提交、SQL原子防超扣、Redisson 分布式锁（事务外） |
| ![用户登录注册](docs/演示素材/用户登录.gif) | 用户登录/注册（JWT 双令牌 + Redis 状态管理） | JWT Access/Refresh 令牌、Redis 服务端状态、可控登出 |
| ![LangSmith 追踪](docs/演示素材/langsmith追踪.gif) | LangSmith 可观测性（Agent 调用链追踪 + Token 消耗分析） | LangSmith Trace、LLM 请求/响应记录、工具调用耗时统计 |

---

## 系统架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                           用户浏览器                                  │
│  ┌──────────────────────────────┐  ┌──────────────────────────┐     │
│  │  用户端前端 (Vue3 + NaiveUI)  │  │  管理端前端 (Vue3+ElPlus)│     │
│  │  :3000                       │  │  :5174                   │     │
│  └────────────┬─────────────────┘  └────────────┬─────────────┘     │
└───────────────┼──────────────────────────────────┼────────────────────┘
                │ HTTP / WebSocket / SSE           │
                ▼                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│            Spring Boot 后端模块 (:8080/api)                          │
│                                                                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ security │ │ article  │ │ comment  │ │  point   │ │  ai      │  │
│  │ 认证授权  │ │ 文章管理  │ │ 评论管理  │ │ 积分系统  │ │ AI 能力  │  │
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
│  ┌────────────────────┐  ┌────────────────────────────────────┐     │
│  │  Writing Agent     │  │  Smart Agent (Supervisor)          │     │
│  │  固定工作流四阶段   │  │  memory_recall → ReAct 循环        │     │
│  │  plan→execute→     │  │  ├─ SearchAgent (create_agent)     │     │
│  │  reflect→finalize  │  │  ├─ KnowledgeAgent (create_agent)   │     │
│  └────────────────────┘  │  └─ CodeExecutionAgent (Judge0)    │     │
│                          └────────────────────────────────────┘     │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  Research Agent (Orchestrator-Worker)                         │    │
│  │  plan_generator → interrupt → Send API 并行 Worker           │    │
│  │  ├─ search_worker (SearchAgent)                              │    │
│  │  ├─ knowledge_worker (KnowledgeAgent)                        │    │
│  │  └─ replanner → reporter                                     │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  共享基础设施层                                               │    │
│  │  services/ → LLM/Embedding/Mem0/Redis记忆/Blog/Task/Skill     │    │
│  │  tools/    → ES搜索/向量检索/作者搜索/代码执行/记忆/搜索管理  │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  LangSmith 可观测性（可选）                                   │    │
│  │  · Agent 调用链追踪（Trace）                                 │    │
│  │  · LLM 请求/响应记录                                         │    │
│  │  · 工具调用耗时统计                                          │    │
│  │  · Token 消耗分析                                            │    │
│  └──────────────────────────────────────────────────────────────┘    │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                     ┌────────┴────────┐
                     ▼                 ▼
               LLM / Embedding        Tavily Search
               (LLM 推理 + Embedding) (外部互联网搜索)
```

---

## 后端模块全景

```
project-backen/  —— Spring Boot 4 + Maven 多模块（17 个子模块）
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
│   ├── AIAPI/                 AICommentApi、MemoryExtractApi、AiArticleDraftApi（AI 接口）
│   ├── interactionAPI/        LikeApi、FollowApi、BrowseHistoryApi（互动接口）
│   ├── messageAPI/            ConversationApi（私信接口）
│   ├── searchAPI/             SearchSyncApi（搜索同步接口）
│   ├── couponAPI/             CouponAPI、BestCouponVO（优惠券接口）
│   ├── vipTccAPI/             VipTccAPI（VIP TCC 补偿接口）
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
│   ├── LikeStrategy 策略模式（文章/评论统一点赞行为）
│   ├── 关注/收藏/浏览历史（通知、私信已拆分到独立模块）
│   ├── config/mqConfig/       InteractionMqConfig（MQ 配置已迁移）
│   └── mqHandler/             BrowseHistoryMqHandler、CollectionMqHandler、FollowMqHandler、LikeMqHandler、UserLikeMqHandler
│
├── blog-coupon/      # 优惠券模块
│   ├── entity/                CouponTemplate、UserCoupon
│   ├── controller/            CouponController、MyCouponController
│   ├── bizService/            CouponBizService（Lua 原子扣减 + 本地消息表）、MqBizService（异步落库 + 三层防超卖）
│   ├── api/                   CouponAPIImpl（跨模块接口实现）
│   ├── config/mqConfig/       CouponMqConfig（MQ 配置）
│   ├── mqHandler/             CouponMqHandler（优惠券消息处理器）
│   └── resources/lua/         coupon_claim.lua（Redis Lua 原子扣减脚本）
│
├── blog-point/       # 积分系统
│   ├── 用户签到（每日签到获取积分）
│   ├── 积分记录（积分收支明细查询）
│   ├── 积分排行榜（用户积分排名）
│   ├── 积分发放（文章发布/点赞/收藏等场景自动发放）
│   ├── config/mqConfig/       PointMqConfig（MQ 配置）
│   └── mqHandler/             PointMqHandler（积分消息处理器）
│
├── blog-vip/         # VIP 会员模块
│   ├── entity/                VipPlan、VipMembership
│   ├── controller/            VipOrderController、VipPlanController、VipStatueController
│   ├── bizService/            OrderBizService（本地事务+分布式锁编排）、VipMqBizService（超时取消）
│   ├── api/                   VipApiImpl（跨模块接口实现）
│   └── config/mqConfig/       VipOrderMqConfig（延迟队列配置）
│
├── blog-search/      # Elasticsearch 全文搜索
│   ├── BoolQuery + MultiMatch 多字段加权搜索（title^2）
│   ├── Completion Suggester 搜索建议
│   ├── config/mqConfig/       SearchMqConfig（MQ 配置已迁移）
│   └── MQ 增量同步
│
├── blog-ai/          # AI 能力集成
│   ├── BizService/          PythonAiChatService（WebClient + Reactor Flux SSE 流式对话）
│   │                        ArticlePolishService / ArticleTitleService / ArticleSummaryService（Spring AI 轻量任务）
│   │                        ContentModerationService（Spring AI 内容审核）
│   │                        AIMemoryService（记忆提取：Redis 扫描 → WebClient 调用 Python → 清理标记）
│   │                        KnowledgeService（知识库管理）/ SkillService（Skills 管理）
│   ├── controller/          对话、写作任务、知识库、Skills、内容审核等 Controller
│   ├── config/              SpringAiConfig（Spring AI 配置）、NacosPromptProperties（@RefreshScope 动态 Prompt）
│   │                        WebClientConfig（WebClient 配置）、PromptManger（Prompt 管理）
│   ├── config/mqConfig/     AiMqConfig（MQ 配置）
│   └── mqHandler/           AiTitleMqHandler（标题生成）、AiModerateMqHandler（内容审核）
│
├── blog-job/         # XXL-Job 定时任务
│   ├── AIMemoryJobHandler       # 记忆提取（每3分钟扫描过期对话，调用 Python 提取记忆）
│   ├── ArticleJobHandler        # 文章统计同步
│   ├── ColumnJobHandler         # 专栏数据同步
│   ├── InteractionJobHandler    # 社交互动数据同步（点赞数、浏览历史）
│   ├── LocalMessageJobHandler   # 本地消息表补偿重试 + 过期清理
│   ├── PointJobHandler          # 积分排行榜刷新
│   └── TccCompensateJobHandler  # TCC 超时事务补偿
│
└── blog-application/ # 启动模块（聚合所有子模块）
```

---

## AI Agent 架构

Python AI Agent 服务（FastAPI :8000）采用**三种 Agent 架构**：
1. **SmartAgent（Supervisor + Sub-Agent）**：调度专业 Sub-Agent，共享底层基础设施层
2. **WritingAgent（固定工作流）**：人工编排的确定性流程，适合写作场景
3. **ResearchAgent（Orchestrator-Worker）**：动态任务生成 + 并行执行，适合深度研究场景

```
project-ai-agent/
│
├── main.py               # FastAPI 入口（SSE + CORS + Nacos 注册 + LangSmith 初始化）
│
├── api/                   # 路由层
│   ├── chat_router.py     # 智能对话 SSE 流式接口
│   ├── writing_router.py  # 写作任务全生命周期接口
│   ├── research_router.py # 深度研究 SSE 流式接口（启动/恢复/停止）
│   ├── knowledge_router.py# 知识库文档上传/管理
│   ├── skill_router.py    # Skills 技能查询
│   └── memory_router.py   # 记忆提取 API（XXL-Job 调用）
│
├── agents/                # LangGraph Agent
│   ├── smart_agent.py     # Supervisor Agent（memory_recall + ReAct 循环推理，调度 Sub-Agent）
│   ├── writing_agent.py   # 固定工作流写作 Agent（人工编排的确定性流程）
│   ├── research_agent.py  # Orchestrator-Worker 深度研究 Agent（Send API 动态并行）
│   └── sub_agents/        # Sub-Agent 模块
│       ├── search_agent.py        # 搜索专家（基于 create_agent）
│       ├── knowledge_agent.py     # 知识库专家（基于 create_agent）
│       ├── code_execution_agent.py # 代码执行专家（Judge0 CE）
│       └── tools.py               # Sub-Agent @tool 注册
│
├── skills/                # Skills 渐进式披露
│   ├── loader.py           # SKILL.md 加载器
│   ├── smart-chat/         # 智能对话
│   ├── search-agent/       # 搜索专家
│   ├── knowledge-agent/    # 知识库问答
│   ├── code-execution-agent/ # 代码执行
│   └── writing-assistant/  # 写作助手
│
├── tools/                 # Agent 工具集
│   ├── __init__.py          # 工具注册表（DIRECT_TOOLS / SUB_AGENT_TOOLS / WRITING_TOOLS）
│   ├── article_tool.py      # ES 文章搜索 + Java API 文章内容获取
│   ├── vector_tool.py       # pgvector 知识库（Parent-Child RAG + category 过滤）
│   ├── author_tool.py       # 作者搜索
│   ├── blog_tool.py         # 分类/标签
│   ├── skill_tool.py        # Skill 详情披露 + 向量化检索工具
│   ├── user_tool.py         # 用户上下文（contextvars 协程安全）
│   ├── writing_tool.py      # 写作任务工具（启动/查询/执行/发布）
│   ├── common_tool.py       # 通用工具（时间/用户信息）
│   ├── memory_tool.py       # 记忆工具（recall_memory / save_memory）
│   ├── code_execution_tool.py # 代码执行工具（Judge0 CE）
│   ├── smart_search_tool.py # 智能搜索管理器（内部+外部搜索协调）
│   ├── web_scraper_tool.py  # 网页爬取工具（trafilatura）
│   └── firecrawl_tool.py    # Firecrawl 网页爬取与搜索
│
├── services/              # 四层服务架构
│   ├── core/                # 基础设施层（LLM/Embedding/Mem0/Nacos/Redis 对话记忆）
│   ├── store/               # 数据存储层（ES/PostgreSQL/pgvector/Parent 文档存储）
│   ├── skill/               # Skill 服务层（SKILL.md 加载器 / 文档分块器）
│   └── business/            # 业务逻辑层（对话/记忆提取/写作/研究/质量/标签/任务/Web 爬取）
│
├── models/                # Pydantic 数据模型
│   ├── writing_models.py  # 写作 Agent 状态模型
│   ├── research_models.py # 研究 Agent 状态模型（ResearchAgentState、WorkerState、ResearchTask）
│   └── schemas.py         # 通用响应模型
├── mcp_service/           # 外部 MCP 服务封装（Firecrawl / Judge0）
├── common/                # 公共常量与基础设施（ES 基类、常量定义）
├── config/
│   ├── settings.py        # 应用配置（含 LangSmith 可观测性配置）
│   └── prompts/           # Prompt 模板集中管理（含 Sub-Agent 提示词）
│   ├── smart_agent_prompts.py  # Supervisor Agent 提示词
│   ├── writing_prompts.py      # 写作 Agent 提示词
│   ├── research_prompts.py     # 研究 Agent 提示词（Planner/Replanner/Reporter）
│   └── sub_agent_prompts.py    # Sub-Agent 提示词
└── vectorstore/           # pgvector 向量存储封装
```

> **Skills 渐进式披露机制**：每个技能定义为独立 `SKILL.md` 文件（YAML frontmatter + Markdown 指南），系统提示词仅含技能名称与一句话描述，Agent 通过 `get_skill_details` 工具按需获取详情。解耦提示词与代码，支持横向扩展 10+ 技能。
>
> **向量化 Skill 节省 Token**：`search_skill_guide` 工具基于 pgvector 语义检索，只返回与当前任务相关的 Skill 指南片段，Token 消耗通常只有完整文档的 20-40%，支持跨 Skill 搜索。
>
> **Skill 三级降级策略**：① 向量检索成功 → 返回格式化切片；② 检索结果不足 → 返回切片 + 降级提示；③ 向量检索异常 → 自动降级加载完整文档，保障可用性。

---

### 写作 Agent 工作流

基于 **LangGraph StateGraph** 的**固定工作流架构**，采用**人工编排的确定性流程**，节点间依赖关系在代码中硬编码，而非由 LLM 动态规划。采用官方推荐的 **Parallelization** 和 **Evaluator-Optimizer** 最佳实践，SSE 实时推送子步骤进度，Redis 管理任务状态支持断点恢复。

> **与标准 Plan-and-Execute 的区别**：标准 Plan-and-Execute 模式中，Planner 动态生成任务列表，Executor 逐步执行，且支持动态调整计划。本项目的写作 Agent 采用**固定流程**：plan → generate_title + generate_tags（并行）→ merge → summary → content → evaluate → revise（循环）→ finalize，所有节点和边在编译时确定，运行时不会动态增删节点。这种设计适合写作场景——流程固定、质量可控、易于调试。

```
┌────────────────────────────────────────────────────────────────────┐
│  Writing Agent — 固定工作流四阶段（人工编排，非动态规划）           │
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
│  │  ┌──────────────┐    ┌──────────────┐                       │  │
│  │  │ generate_title│   │ generate_tags│  ← 并行执行              │  │
│  │  │ 生成标题       │   │ 分类标签       │    LangGraph 原生并行    │  │
│  │  │ (ContentSvc)  │   │ (TagSvc)      │                       │  │
│  │  └──────┬───────┘    └──────┬───────┘                       │  │
│  │         │                   │                                │  │
│  │         └──────────┬────────┘                                │  │
│  │                    │  Annotated[list, add] reducer          │  │
│  │                    │  自动合并并行输出到 parallel_outputs     │  │
│  │                    ▼                                         │  │
│  │             ┌────────────────┐                               │  │
│  │             │ merge_title_tags│ ← 合并标题+标签                │  │
│  │             │    汇合点       │   纯逻辑，无外部调用             │  │
│  │             └───────┬────────┘                               │  │
│  │                     │                                        │  │
│  │             ┌───────▼────────┐                               │  │
│  │             │ generate_summary│ ← 生成摘要                    │  │
│  │             │  (ContentSvc)   │                              │  │
│  │             └───────┬────────┘                               │  │
│  │                     │                                        │  │
│  │             ┌───────▼────────┐                               │  │
│  │             │ generate_content│ ← 生成正文                    │  │
│  │             │  (ContentSvc)   │                              │  │
│  │             └───────┬────────┘                               │  │
│  └───────────────────┼──────────────────────────────────────────┘  │
│                      │                                             │
│                      ▼          LLM temperature = 0.6（技术博客）  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  REFLECT 反思阶段（Evaluator-Optimizer 循环）                 │  │
│  │                                                              │  │
│  │  ┌──────────────┐                                            │  │
│  │  │  evaluate    │  ← 5 维评分：完整性(30%) + 结构性(20%)      │  │
│  │  │ (QualitySvc) │              + 表达(25%) + 实用性(15%)      │  │
│  │  └──────┬───────┘              + 格式(10%)                    │  │
│  │         │                                                     │  │
│  │         │  评分 < 7.0 且修订次数 < 3                          │  │
│  │         │                                                     │  │
│  │         ▼                                                     │  │
│  │  ┌──────────────┐                                            │  │
│  │  │   revise     │  ← 精细化修订（针对性修改）                  │  │
│  │  │ (QualitySvc) │                                            │  │
│  │  └──────┬───────┘                                            │  │
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

**架构亮点：**

| 特性 | 实现方式 | 官方文档参考 |
|------|---------|-------------|
| **原生并行模式** | `Annotated[list, operator.add]` reducer 自动合并并行输出 | [Parallelization](https://docs.langchain.com/oss/python/langgraph/workflows-agents#parallelization) |
| **Evaluator-Optimizer 循环** | evaluate → revise → evaluate 循环，精细化修订 | [Evaluator-optimizer](https://docs.langchain.com/oss/python/langgraph/workflows-agents#evaluator-optimizer) |
| **节点单一职责** | 每个节点只做一件事，State 存储原始数据 | [Thinking in LangGraph](https://docs.langchain.com/oss/python/langgraph/thinking-in-langgraph) |
| **SSE 流式推送** | 实时推送各阶段进度，前端实时更新 | [Event streaming](https://docs.langchain.com/oss/python/langgraph/event-streaming) |
| **Human-in-the-loop** | `interrupt_before` 等待用户确认计划 | [Persistence](https://docs.langchain.com/oss/python/langgraph/persistence) |

---

### 深度研究 Agent — Orchestrator-Worker 架构

基于 **LangGraph Orchestrator-Worker 模式**，参考[官方文档](https://docs.langchain.com/oss/python/langgraph/workflows-agents#orchestrator-worker)实现。与写作 Agent 的固定流程不同，研究 Agent 采用**动态任务生成 + 并行执行**的架构，Planner 根据研究主题动态生成任务列表，通过 Send API 为每个任务创建独立 Worker 并行执行。

**核心设计原则：**

- **assign_workers 是条件边函数**（非节点），返回 `Send` 列表，为每个 pending 任务动态创建 Worker
- **Worker 使用独立的 WorkerState**，只接收单个任务，通过 `completed_tasks: Annotated[list, add]` reducer 自动合并结果到主状态
- **Replanner 评估结果**，决定继续执行补充任务或进入报告生成
- **interrupt 人工审批**：计划生成后等待用户确认，支持拒绝并携带修改意见重新生成
- **硬性终止条件**：任务总数上限（8）、迭代轮次上限（3）、全局时间上限（15分钟）

**架构总览：**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ResearchAgent — Orchestrator-Worker 模式                               │
│                                                                         │
│  ┌──────────────┐                                                       │
│  │ plan_generator│  LLM 分析需求 → 结构化研究计划（主题/任务列表）        │
│  │   计划生成     │  SSE → plan_approval 事件                            │
│  └───────┬──────┘  LLM temperature = 0.1（低温精确规划）                │
│          │                                                               │
│          ▼                                                               │
│  ┌──────────────┐                                                       │
│  │plan_await_user│  interrupt() 等待用户确认/拒绝/澄清                   │
│  │   等待审批     │  用户确认 → plan_approved → 进入执行                  │
│  └───────┬──────┘  用户拒绝 → needs_replan → 返回 plan_generator        │
│          │                                                               │
│          ▼          条件边函数 _route_after_plan_await_user              │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  Worker 并行执行阶段（Send API 动态创建）                          │  │
│  │                                                                   │  │
│  │  _create_worker_sends() 遍历 tasks 列表，为每个 pending 任务       │  │
│  │  创建 Send("search_worker"/"knowledge_worker", {"task": task})    │  │
│  │                                                                   │  │
│  │  ┌─────────────────┐    ┌─────────────────┐                      │  │
│  │  │  search_worker  │    │knowledge_worker │  ← 并行执行           │  │
│  │  │  搜索 Worker    │    │  知识库 Worker   │    Send API           │  │
│  │  │                 │    │                 │                       │  │
│  │  │ · ES 文章搜索   │    │ · RAG 知识库    │                       │  │
│  │  │ · 外部网页爬取  │    │ · pgvector 检索 │                       │  │
│  │  │ · Tavily 搜索   │    │                 │                       │  │
│  │  └────────┬────────┘    └────────┬────────┘                      │  │
│  │           │                      │                                │  │
│  │           └──────────┬───────────┘                                │  │
│  │                      │  Annotated[list, add] reducer             │  │
│  │                      │  自动合并到 completed_tasks                │  │
│  │                      ▼                                            │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                         │                                               │
│                         ▼                                               │
│  ┌──────────────┐                                                       │
│  │  replanner   │  评估执行结果，决定下一步                              │
│  │   评估决策    │  · 合并 completed_tasks 到 tasks 列表                 │
│  │              │  · 检查硬性终止条件（任务数/轮次/时间）                 │
│  └───────┬──────┘  · LLM 评估：complete → reporter / continue → 补充任务│
│          │          LLM temperature = 0.3（低温评估）                    │
│          │                                                               │
│          ▼          条件边函数 _route_after_replanner                    │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  路由决策：                                                       │  │
│  │  · ready_to_report → reporter（生成报告）                         │  │
│  │  · executing + pending_tasks → _create_worker_sends（继续执行）   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│          │                                                               │
│          ▼                                                               │
│  ┌──────────────┐                                                       │
│  │   reporter   │  整合所有发现，生成研究报告                            │
│  │   报告生成    │  · 全局视角：所有任务结果 + 关键发现                   │
│  │              │  · 结构化报告：内容/摘要/关键发现/来源                  │
│  └───────┬──────┘  · 回调 Java 后端持久化报告                           │
│          │          LLM temperature = 0.4（中温整合）                    │
│          │                                                               │
│          ▼                                                               │
│         END                                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

**Worker 类型（仅两种，职责清晰）：**

| Worker | 职责 | 调用的 Agent | 工具 |
|--------|------|-------------|------|
| **Search Worker** | 搜索站内文章/外部网页 | SearchAgent | ES 搜索、Tavily、Firecrawl |
| **Knowledge Worker** | 查询知识库 | KnowledgeAgent | pgvector RAG 检索 |

> **与写作 Agent 的架构差异**：写作 Agent 采用固定流程（plan → title + tags → merge → summary → content → evaluate → finalize），所有节点和边在编译时确定；研究 Agent 采用 Orchestrator-Worker 模式，通过 Send API 动态创建 Worker，任务数量和类型在运行时根据研究主题动态决定，支持多轮迭代补充任务。

**硬性终止条件（防止无限循环）：**

| 条件 | 阈值 | 说明 |
|------|------|------|
| MAX_TASKS | 10 | 任务总数上限 |
| MAX_ROUNDS | 3 | Replanner 迭代轮次上限 |
| MAX_DURATION | 900s (15min) | 全局执行时间上限 |

**SSE 事件流：**

| 事件类型 | 触发时机 | 数据内容 |
|---------|---------|---------|
| `phase` | 阶段切换 | phase: planning/evaluating/reporting/completed |
| `planner_thinking` | Planner 思考过程 | LLM 思维链输出 |
| `plan_approval` | 计划生成完成 | 研究计划详情 |
| `clarification` | 需求模糊需澄清 | 澄清问题列表 |
| `task_progress` | 任务执行进度 | task_id/status/search_count/scrape_count |
| `stage_insight` | Replanner 评估洞察 | round/insight/completed_tasks/total_tasks |
| `replan` | 补充任务生成 | round/reason/added_tasks/updated_task_list |
| `report_ready` | 报告生成完成 | topic/summary/key_findings/content |

**关键实现细节：**

| 特性 | 实现方式 | 代码位置 |
|------|---------|---------|
| **条件边函数返回 Send** | `_route_after_plan_await_user` / `_route_after_replanner` 返回 `Send` 列表 | `research_agent.py:159-198` |
| **Worker 独立状态** | Worker 使用 `WorkerState`（只含 task 字段），通过 reducer 合并 | `research_models.py:WorkerState` |
| **interrupt 拆分为两个节点** | `plan_generator`（不含 interrupt）+ `plan_await_user`（仅 interrupt），遵循官方原则"side effects before interrupt must be idempotent" | `research_agent.py:110-155` |
| **中断恢复** | `resume_research()` 使用 `Command(resume=response)` 恢复执行 | `research_agent.py:757-787` |
| **任务状态机** | pending → executing → completed/failed | `research_models.py:ResearchTask` |

---

### Smart Agent — Supervisor + Sub-Agent 架构

基于 **LangGraph StateGraph** 的 **Supervisor (tool-calling)** 多 Agent 架构。SmartAgent 作为 Supervisor，通过 LLM tool-calling 自主决定调用哪个专业 Sub-Agent 或直接使用工具，Sub-Agent 基于 `create_agent` 预构建 ReAct 循环，无需手写图。入口为 `memory_recall_node`，首轮对话自动注入用户长期记忆（Mem0），后续轮次跳过召回进入 ReAct 推理循环。内置三层记忆系统：短期（LangGraph Checkpointer）、中期（LangMem 增量压缩）、长期（Mem0）。

**架构总览：**

```
┌─────────────────────────────────────────────────────────────────────┐
│  SmartAgent（Supervisor）— memory_recall + ReAct 循环推理             │
│                                                                     │
│  memory_recall ──→ thinking ──→ judge ──→ tool_executor ──→ thinking│
│       │                   │                                         │
│       │                   └──→ (无 tool_calls) ──→ END              │
│       ▼                                                             │
│    首轮召回用户记忆，注入上下文                                       │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  DIRECT_TOOLS（Supervisor 直接调用）                         │    │
│  │  get_skill_details / list_available_skills / search_skill_guide│  │
│  │  recall_memory / save_memory                                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  SUB_AGENT_TOOLS（@tool 包装的专业 Agent）                   │    │
│  │                                                             │    │
│  │  ┌──────────────────┐    ┌──────────────────┐              │    │
│  │  │ search_agent     │    │ knowledge_agent  │              │    │
│  │  │ 搜索专家          │    │ 知识库专家        │              │    │
│  │  │ (create_agent)   │    │ (create_agent)   │              │    │
│  │  │                  │    │                  │              │    │
│  │  │ · 文章搜索        │    │ · RAG 知识库检索  │              │    │
│  │  │ · 博主搜索        │    │                  │              │    │
│  │  │ · 分类查询        │    │                  │              │    │
│  │  │ · 外部博客搜索    │    │                  │              │    │
│  │  │ · 网页爬取        │    │                  │              │    │
│  │  └──────────────────┘    └──────────────────┘              │    │
│  │                                                             │    │
│  │  ┌──────────────────┐                                      │    │
│  │  │ code_execution   │                                      │    │
│  │  │ 代码执行专家      │                                      │    │
│  │  │ (Judge0 CE)      │                                      │    │
│  │  │                  │                                      │    │
│  │  │ · 60+ 语言执行    │                                      │    │
│  │  │ · 沙箱隔离        │                                      │    │
│  │  └──────────────────┘                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  WRITING_TOOLS（异步两阶段写作流程）                          │    │
│  │  writing_start / writing_status / writing_action             │    │
│  │  writing_result / writing_publish                            │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

**Supervisor 核心机制：**

- **记忆召回节点**：`memory_recall_node` 作为图入口，首轮对话自动从 Mem0 召回用户记忆（语义/情节/程序三类），通过 SystemMessage 注入上下文；内置三层记忆：短期（LangGraph Checkpointer 完整消息）、中期（LangMem 增量压缩超长对话）、长期（Mem0 语义/情节/程序记忆）；`recall_memory` / `save_memory` 工具支持 LLM 按需召回和主动存储
- **LLM 思考模式**：`reasoning_content`（思维链）→ `thinking` 事件，`content`（最终回答）→ `chunk` 事件，天然分离无需额外解析
- **LangGraph 原生 custom 流模式**：thinking 节点通过 `get_stream_writer()` 实时发射 thinking / chunk / tool_call 事件，零缓存回放
- **Sub-Agent 事件转发**：Sub-Agent 的工具调用事件通过 `stream_writer` 转发到 Supervisor，显示在前端思考块中
- **add_messages reducer**：节点只需返回新消息，LangGraph 自动追加到消息历史
- **并发工具调用**：`asyncio.gather` 同时执行多个工具调用
- **迭代次数控制**：超过最大迭代次数强制结束，防止无限循环

**Sub-Agent 设计：**

| 特性 | 实现方式 |
|------|---------|
| **预构建 ReAct 循环** | `langchain.agents.create_agent` 一行创建，无需手写 StateGraph |
| **独立工具集** | SearchAgent 拥有搜索相关工具，KnowledgeAgent 拥有 RAG 检索工具，CodeExecutionAgent 拥有代码执行工具 |
| **非思考模式** | LLM 快速响应模式（thinking: disabled），快速响应无需思维链分层 |
| **流式事件转发** | `stream_mode=["updates", "custom"]` + `version="v2"` 提取工具调用事件 |
| **单例模式** | `get_search_agent()` / `get_knowledge_agent()` 全局单例 |

**工具分类总览：**

| 分类 | 工具 | 功能 | 来源 |
|------|------|------|------|
| **DIRECT_TOOLS** | `get_skill_details` | 获取 Skill 详细使用指南 | Skills 文件 |
| | `list_available_skills` | 列出所有可用 Skills | Skills 文件 |
| | `search_skill_guide` | 语义搜索 Skill 指南片段 | pgvector |
| | `recall_memory` | 按需召回用户历史记忆（语义/情节/程序） | Mem0 LongTermMemoryService |
| | `save_memory` | 主动保存重要信息到用户记忆 | Mem0 LongTermMemoryService |
| **SUB_AGENT_TOOLS** | `search_agent` | 搜索专家（文章/博主/分类/外部搜索/爬取） | SearchAgent |
| | `knowledge_agent` | 知识库专家（RAG 语义检索） | KnowledgeAgent |
| | `code_execution_agent` | 代码执行专家（60+ 语言，Judge0 沙箱） | CodeExecutionAgent |
| **WRITING_TOOLS** | `writing_start` | 启动写作任务，异步生成计划 | WritingAgent |
| | `writing_status` | 查询写作任务状态及计划内容 | WritingAgent |
| | `writing_action` | 执行写作动作（确认/修订/取消） | WritingAgent |
| | `writing_result` | 获取写作成果链接 | WritingAgent |
| | `writing_publish` | 发布或保存草稿 | WritingAgent |

**SearchAgent 内部工具：**

| 工具 | 功能 | 来源 |
|------|------|------|
| `search_articles_by_keyword` | ES 关键词搜索文章 | Elasticsearch |
| `get_hot_articles` | 获取浏览量最高的热门文章 | Elasticsearch |
| `search_authors_by_keyword` | ES 博主搜索 | Elasticsearch |
| `get_hot_authors` | 获取粉丝数最多的热门博主 | Elasticsearch |
| `get_author_by_id` | 根据 ID 获取博主详细信息 | Elasticsearch |
| `get_category_list` | 获取所有文章分类 | Java API |
| `scrape_webpage` | 爬取网页内容转 Markdown | Web 爬虫 |
| `search_external_tech_blogs` | 站外技术博客搜索（条件加载） | Tavily |

**KnowledgeAgent 内部工具：**

| 工具 | 功能 | 来源 |
|------|------|------|
| `search_knowledge_base` | Parent-Child RAG 语义检索（支持 category 元数据过滤：project/interview/general） | pgvector |

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
│  用户提问 → category 意图识别          │
│         → 语义检索 Top-K Child（按 category 过滤）│
│         → 提取 doc_id（去重）        │
│         → 批量获取完整 Parent Doc    │
│         → LLM 综合回答              │
└──────────────────────────────────────┘
```

**知识库三层隔离架构：**

| 隔离层 | 机制 | 目的 |
|--------|------|------|
| **Category 逻辑隔离** | 文档上传时指定 category（project/interview/general），检索时通过元数据过滤 | 不同场景知识互不干扰 |
| **pgvector 物理分表** | blog_knowledge（知识库文档）、skill_chunks（Skill 文档）、user_memories（Mem0 用户记忆） | 数据类型隔离，独立维护 |
| **Mem0 用户级隔离** | 以 user_id 为隔离键，每个用户的长期记忆独立存储与检索 | 多用户记忆安全 |

---

## 全文搜索

ByteBlog 采用 **Elasticsearch** 实现毫秒级全文搜索，支持文章/作者/专栏三类内容的统一检索。

**核心能力：**

| 功能 | 说明 |
|------|------|
| **BoolQuery + MultiMatch** | 多字段加权检索（title^2），精准匹配标题与内容 |
| **Completion Suggester** | 搜索建议，输入即提示，提升搜索体验 |
| **MQ 增量同步** | 文章发布/更新时通过 RabbitMQ 异步同步到 ES，保障数据一致性 |
| **关键词高亮** | 搜索结果中关键词高亮展示，快速定位相关内容 |

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

---

## 持久层策略

| 存储 | 用途 | 策略 |
|------|------|------|
| **PostgreSQL** | 业务数据持久化 | 雪花 ID 主键（@JsonSerialize ToStringSerializer 防前端精度丢失），MyBatis Plus ORM |
| **本地消息表** | MQ 可靠投递 | tb_local_message 表，业务操作与消息同一事务，trySend + XXL-Job 定时补偿，指数退避重试，乐观锁防重复发送 |
| **TCC 事务记录表** | 分布式事务状态管理 | tb_tcc_transaction 表，分支状态机（TRYING→CONFIRMED/CANCELLED），空回滚记录防悬挂，幂等校验 |
| **pgvector** | 向量数据 | 同一 PostgreSQL 实例 + vector 扩展，Parent-Child RAG，余弦相似度搜索 |
| **Caffeine（L1）** | 本地进程内缓存 | 原子 get 加载防击穿，CacheEntry 键级 TTL，各模块独立实例（容量 10~500，TTL 2min~1h） |
| **Redis（L2）** | 分布式缓存 + 计数器 + 在线状态 + 分布式锁 | NULL_VALUE 占位符防穿透，GenericJacksonJsonRedisSerializer 序列化 |
| **RabbitMQ** | 异步解耦 | 16 个队列 + 对应死信队列，统一 DlqRetryHandler 重试，手动 ACK |
| **Elasticsearch** | 全文搜索 | 3 类文档索引（文章/作者/专栏），BoolQuery + MultiMatch，MQ 增量同步 |
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
| 前端用户端 | 3000 |
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
| **LANGCHAIN_TRACING_V2** | 是否开启 LangSmith 追踪 | `false` | 可选 |
| **LANGCHAIN_API_KEY** | LangSmith API 密钥 | — | 可选 |
| **LANGCHAIN_PROJECT** | LangSmith 项目名称 | `byteblog-agent-dev` | 可选 |
| **LANGCHAIN_ENDPOINT** | LangSmith API 端点 | `https://api.smith.langchain.com` | 可选 |

> ⚠️ **重要**：Python Agent 的 `BACKEND_API_KEY` 必须与 Java 后端的 `API_UNIVERSAL_KEY` 设置为相同值。
>
> **LangSmith 可观测性**：开启 `LANGCHAIN_TRACING_V2=true` 后，所有 Agent 调用链（包括 LLM 请求、工具调用、记忆召回等）会自动上报到 LangSmith 平台，便于调试和性能分析。详见 [LangSmith 文档](https://docs.smith.langchain.com/)。

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
├── project-backen/           # Spring Boot 后端（17 个 Maven 子模块）
├── project-front/            # 前端用户端（Vue 3 + NaiveUI + Tailwind）
├── project-front-admin/      # 前端管理端（Vue 3 + Element Plus）
├── project-ai-agent/         # Python AI Agent（FastAPI + LangGraph）
├── sql/                      # 数据库 DDL 脚本
├── docker/                   # Docker 配置（Prometheus + Grafana 监控栈）
├── docs/                     # 项目文档
│   ├── 演示素材/             # 功能演示 GIF
│   ├── 模块文档/             # 模块设计文档
│   ├── API文档/              # 接口文档
│   ├── 代码评审/             # 代码评审报告
│   └── 项目概览/             # 项目知识库
├── .gitignore                # Git 忽略规则
├── AGENTS.md                 # AI 辅助开发规则
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
| 积分模块接口文档 | 签到、积分记录、排行榜 |
| 优惠券模块接口文档 | 优惠券领取、我的优惠券、优惠券详情 |
| VIP 模块接口文档 | VIP 套餐、订单管理、会员状态 |
| 搜索模块接口文档 | 全文搜索、搜索建议 |
| 管理端接口文档目录 | 认证/仪表盘/文章/分类/标签/评论/用户/专栏/通知/日志管理 |

### Python AI Agent API

启动后访问 http://localhost:8000/docs 在线查看。

| 路由前缀 | 功能 |
|---------|------|
| `/api/v1/chat` | 智能对话（SSE 流式 + 工具调用 + 深度思考 + 记忆召回） |
| `/api/v1/writing` | AI 写作（固定工作流 + SSE 流式进度） |
| `/api/v1/research` | 深度研究（Orchestrator-Worker + SSE 流式进度 + 人工审批） |
| `/api/v1/knowledge` | 知识库文档上传/管理（支持 category 分类） |
| `/api/v1/skill` | Skills 技能查询（渐进式披露 + 向量化检索） |
| `/api/v1/memory` | 记忆提取 API（XXL-Job 跨服务调用，批量提取对话记忆） |
