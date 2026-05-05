# 个人博客系统知识库

> **项目作者**: 李帅豪 (LSH)  
> **项目性质**: 个人展示项目，用于学习交流  
> **联系方式**: 仅供学习交流使用，请勿用于商业用途

---

## 项目声明

本项目 **ByteBlog 个人博客系统** 是李帅豪的个人学习展示项目，旨在学习和实践现代 Web 开发技术栈。项目完全开源，供广大开发者学习交流使用。

**学习交流范围包括但不限于**：
- Spring Boot 多模块架构设计
- Vue 3 组合式 API 开发实践
- AI 应用开发（Spring AI + LangChain）
- 前后端分离架构
- 微服务设计模式
- 数据库设计与优化

**注意事项**：
- 本项目仅供学习交流，请勿用于商业用途
- 欢迎提出 Issue 和 Pull Request
- 如有问题欢迎在 GitHub 上讨论

---

## 一、项目概述

### 1.1 项目简介

ByteBlog 是一个基于前后端分离架构的现代化个人博客系统，采用 Vue 3 + Spring Boot 主流技术栈打造，集内容管理、智能创作、全文检索、互动评论于一体。

### 1.2 核心功能

| 功能模块 | 描述 |
|---------|------|
| 文章管理 | 文章发布、编辑、删除、分类、标签管理 |
| 评论系统 | 文章评论、回复、点赞 |
| 用户互动 | 关注、私信、点赞、收藏、通知 |
| 问答社区 | 问题发布、回答、采纳最佳答案 |
| 全文搜索 | Elasticsearch 文章、问题、作者搜索 |
| AI 助手 | 智能对话、RAG 知识问答、内容创作辅助 |

### 1.3 项目结构

```
person-blog/
├── project-backen/          # 后端项目 (Spring Boot 多模块)
│   ├── blog-application/    # 启动模块
│   ├── blog-common/         # 公共模块
│   ├── blog-api/            # 跨模块调用 API 接口定义
│   ├── blog-security/       # 安全认证模块
│   ├── blog-article/        # 文章管理模块
│   ├── blog-comment/        # 评论管理模块
│   ├── blog-interaction/    # 用户互动模块
│   ├── blog-question/       # 问答模块
│   ├── blog-search/         # 搜索服务模块
│   ├── blog-ai/             # AI 服务模块
│   └── blog-job/            # 任务调度模块
├── project-ai-agent/        # Python AI Agent 服务
├── project-front/           # 前端项目 (用户端 Vue 3)
├── project-front-admin/     # 前端项目 (管理端 Vue 3)
├── docs/                    # 项目文档
└── sql/                     # 数据库脚本
```

---

## 二、技术栈

### 2.1 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | Java 版本 |
| Spring Boot | 4.0.4 | 核心框架 |
| MyBatis Plus | 3.5.16 | ORM 框架 |
| PostgreSQL | 18 | 数据库，支持 JSON 类型和全文搜索/RAG向量库 |
| Redis | 6.0.16 | 缓存、会话存储、分布式锁 |
| RabbitMQ | 3.12+ | 消息队列，异步任务、邮件通知 |
| XXL-Job | 2.4+ | 分布式任务调度 |
| Elasticsearch | 8.11+ | 全文检索、智能推荐 |
| Spring Security | 6.2+ | 安全框架，JWT 令牌管理 |
| Spring AI | 2.0.0-M4 | AI 集成框架 |

### 2.2 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4+ | 渐进式 JavaScript 框架，组合式 API |
| Vite | 5.0+ | 下一代前端构建工具 |
| TypeScript | 5.0+ | 类型安全的 JavaScript 超集 |
| Pinia | 2.1+ | Vue 官方状态管理库 |
| Vue Router | 4.2+ | Vue 官方路由管理器 |
| Element Plus | 2.4+ | 基于 Vue 3 的企业级 UI 组件库 |
| Tailwind CSS | 3.4+ | CSS 框架 |

### 2.3 Python AI 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Python | 3.13 | Python 版本 |
| FastAPI | 0.109+ | 高性能异步 Web 框架 |
| LangChain | 0.1+ | LLM 应用开发框架 |
| LangGraph | 0.0.20+ | Agent 工作流编排框架 |
| pgvector | 0.2+ | PostgreSQL 向量扩展 |

---

## 三、技术亮点详解

### 3.1 Spring Boot 多模块架构设计

本项目采用 Maven 多模块架构，实现了高内聚、低耦合的模块化设计。

#### 3.1.1 模块分层架构

```
blog-common (基础层，无依赖)
    ↓
blog-api (API 接口层，依赖 blog-common)
    ↓
blog-security, blog-article, blog-comment, blog-interaction, blog-question, blog-search, blog-ai, blog-job (业务层)
    ↓
blog-application (启动层，聚合所有模块)
```

#### 3.1.2 模块职责划分

| 模块 | 职责 | 设计理念 |
|------|------|----------|
| blog-common | 工具类、常量、统一响应、异常处理 | 基础设施层，零业务依赖 |
| blog-api | 跨模块调用 API 接口定义 | 解决模块间循环依赖，定义契约 |
| blog-security | 用户认证、JWT、权限管理 | 安全关注点分离 |
| blog-article | 文章、分类、标签管理 | 内容管理核心 |
| blog-comment | 评论发布、审核、回复 | 异步处理，削峰填谷 |
| blog-interaction | 关注、私信、点赞、收藏、通知 | 社交功能聚合 |
| blog-question | 问答管理 | 社区互动 |
| blog-search | 全文检索、智能推荐 | 搜索引擎集成 |
| blog-ai | AI 辅助创作 | 智能化扩展 |
| blog-job | 定时任务 | 任务调度中心 |

#### 3.1.3 跨模块调用解决方案

通过 `blog-api` 模块定义接口契约，服务提供方实现接口，服务消费方依赖接口：

```java
// blog-api 模块定义接口
public interface UserInfoApi {
    List<UserDTO> getUserInfo(Collection<Long> userIds);
}

// blog-security 模块实现接口
@Service
public class UserServiceImpl implements IUserService, UserInfoApi {
    @Override
    public List<UserDTO> getUserInfo(Collection<Long> userIds) {
        // 实现逻辑
    }
}

// blog-article 模块使用接口
@Service
public class ArticleServiceImpl {
    private final UserInfoApi userInfoApi;
    
    public void someMethod() {
        List<UserDTO> users = userInfoApi.getUserInfo(userIds);
    }
}
```

**技术亮点**：
- 解决了模块间循环依赖问题
- 实现了模块间的松耦合
- 便于单元测试和模块独立部署

#### 3.1.4 Maven 依赖管理

```xml
<!-- 父 POM 依赖管理 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>4.0.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 业务模块依赖示例 -->
<dependencies>
    <dependency>
        <groupId>com.personblog</groupId>
        <artifactId>blog-common</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.personblog</groupId>
        <artifactId>blog-api</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

---

### 3.2 AI 双引擎架构设计

本项目创新性地采用 **Spring AI + Python(LangChain/LangGraph)** 双引擎架构，充分发挥两种技术栈的优势。

#### 3.2.1 技术选型对比

| 技术栈 | 适用场景 | 核心优势 |
|--------|----------|----------|
| Spring AI | 简单AI调用、同步处理、文本摘要、内容审核 | 与Spring Boot无缝集成、类型安全、开发效率高 |
| Python + LangChain + LangGraph | 复杂Agent工作流、RAG、多步骤推理 | 丰富的AI生态、灵活的工作流编排、社区活跃 |

#### 3.2.2 功能模块映射

| 功能模块 | 技术栈选择 | 选择理由 |
|----------|------------|----------|
| AI 智能对话 | Spring AI | 简单对话，需要与 Spring Boot 无缝集成 |
| RAG 知识问答 | Python + LangGraph | 复杂检索增强生成，支持多轮对话 |
| 文章发布 Agent | Python + LangGraph | 多步骤工作流（解析→生成→审核） |
| 评论总结 | Spring AI | 简单文本摘要，快速响应 |
| 内容审核 | Spring AI | 同步处理，快速响应 |

#### 3.2.3 服务通信架构

```
┌─────────────────┐      Feign Client      ┌─────────────────┐
│   Spring Boot   │ ──────────────────────> │  Python FastAPI │
│   (blog-ai)     │ <────────────────────── │  (AI Agent)     │
└─────────────────┘      JSON/HTTP          └─────────────────┘
```

#### 3.2.4 Feign Client 配置

```java
@FeignClient(name = "python-ai-service", url = "${ai.python-service.url}")
public interface PythonAiClient {
    
    @PostMapping("/api/v1/rag/query")
    RAGResponse ragQuery(@RequestBody RAGQueryRequest request);
    
    @PostMapping("/api/v1/agent/article/publish")
    ArticlePublishResponse publishArticle(@RequestBody ArticlePublishRequest request);
}
```

**技术亮点**：
- 根据场景选择最合适的技术栈
- Java 端负责业务逻辑和简单 AI 任务
- Python 端负责复杂 AI 工作流
- 统一响应格式，无缝集成

---

### 3.3 Spring AI 智能对话实现

#### 3.3.1 核心架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           blog-ai 模块                                   │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                      Spring AI 核心层                              │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐  │  │
│  │  │ ChatClient │  │ VectorStore│  │ Tool/Func  │  │ ChatMemory │  │  │
│  │  └────────────┘  └────────────┘  └────────────┘  └────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│  PostgreSQL   │          │    Redis      │          │  OpenAI API   │
│  (pgvector)   │          │  (会话缓存)    │          │   (LLM服务)   │
└───────────────┘          └───────────────┘          └───────────────┘
```

#### 3.3.2 多轮对话上下文管理

使用 Spring AI 的 `ChatMemory` 机制实现多轮对话上下文记忆：

```java
@Service
public class ChatService {
    
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    
    public Flux<ChatEventVO> chatStream(Long conversationId, String content) {
        String threadId = conversationId.toString();
        
        return chatClient.prompt()
                .system("你是博客系统的AI助手...")
                .user(content)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(threadId)
                        .build())
                .stream()
                .chatResponse()
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();
                    return ChatEventVO.data(text);
                });
    }
}
```

#### 3.3.3 Redis ChatMemory 配置

```java
@Configuration
public class ChatMemoryConfig {
    
    @Bean
    public ChatMemory chatMemory(RedisTemplate<String, Object> redisTemplate) {
        return new RedisChatMemory(redisTemplate);
    }
}

public class RedisChatMemory implements ChatMemory {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "chat:memory:";
    
    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        List<Map<String, String>> messageData = messages.stream()
                .map(this::toMap)
                .toList();
        redisTemplate.opsForList().rightPushAll(key, messageData.toArray());
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }
    
    @Override
    public List<Message> get(String conversationId, int lastN) {
        String key = KEY_PREFIX + conversationId;
        long size = redisTemplate.opsForList().size(key);
        long start = Math.max(0, size - lastN);
        List<Object> data = redisTemplate.opsForList().range(key, start, size - 1);
        return data.stream()
                .map(this::toMessage)
                .toList();
    }
}
```

**技术亮点**：
- 使用 Redis 存储 ChatMemory，支持分布式部署
- 自动管理对话历史，无需手动拼接上下文
- 支持流式响应（SSE），实现打字机效果

#### 3.3.4 SSE 流式响应实现

后端使用 Spring WebFlux 的 `Flux` 实现流式响应：

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ChatEventVO> chatStream(@RequestParam Long conversationId, 
                                     @RequestParam String content) {
    return chatService.chatStream(conversationId, content)
            .concatWith(Flux.just(ChatEventVO.STOP_EVENT));
}
```

前端使用 Fetch API + ReadableStream 处理 SSE：

```javascript
const reader = stream.getReader()
const decoder = new TextDecoder()
let buffer = ''

while (true) {
  const { done, value } = await reader.read()
  if (done) break
  
  buffer += decoder.decode(value, { stream: true })
  const parts = buffer.split('\n\n')
  buffer = parts.pop() || ''
  
  for (const part of parts) {
    if (part.startsWith('data:')) {
      const event = JSON.parse(part.substring(5).trim())
      if (event.eventType === 1) {
        assistantMessage.content += event.eventData
      }
    }
  }
}
```

---

### 3.4 Agent 工作流模式

Spring AI 2.0.0-M4 原生支持 5 种 Agent 工作流模式，本项目根据不同场景选择合适的模式。

#### 3.4.1 工作流模式概览

| 模式 | 说明 | 适用场景 | 博客应用 |
|------|------|----------|----------|
| Chain Workflow | 链式工作流，顺序执行多步骤 | 任务有明确顺序步骤 | 内容审核流程 |
| Parallelization Workflow | 并行工作流，同时处理多个任务 | 独立任务并发处理 | 文章多维度分析 |
| Routing Workflow | 路由工作流，根据输入分发 | 不同类型输入需专门处理 | 智能客服路由 |
| Orchestrator-Workers | 编排器-工作者，动态拆分子任务 | 子任务无法预先确定 | 智能内容创作 |
| Evaluator-Optimizer | 评估-优化模式，迭代改进结果 | 需要迭代优化的任务 | 文章质量优化 |

#### 3.4.2 Chain Workflow 示例：内容审核

```java
@Component
public class ContentReviewChainWorkflow {
    
    private final ChatClient chatClient;
    
    private static final String[] SYSTEM_PROMPTS = {
        "你是内容审核专家。检查内容是否包含敏感词...",
        "你是内容质量评估专家。评估内容的专业性...",
        "你是审核报告生成专家。生成完整的审核报告..."
    };
    
    public String review(String content) {
        String response = content;
        for (String prompt : SYSTEM_PROMPTS) {
            response = chatClient.prompt()
                    .user(prompt + "\n\n内容：\n" + response)
                    .call()
                    .content();
        }
        return response;
    }
}
```

#### 3.4.3 Parallelization Workflow 示例：文章分析

```java
@Component
public class ArticleAnalysisParallelWorkflow {
    
    public ArticleAnalysisResult analyze(String articleContent) {
        List<String> analysisTasks = List.of(
            "从技术深度角度分析...",
            "从可读性角度分析...",
            "从SEO角度分析..."
        );
        
        List<CompletableFuture<String>> futures = analysisTasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> 
                    chatClient.prompt()
                        .user(task + "\n\n文章内容：\n" + articleContent)
                        .call()
                        .content()
                ))
                .toList();
        
        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        
        return new ArticleAnalysisResult(results.get(0), results.get(1), results.get(2));
    }
}
```

#### 3.4.4 Routing Workflow 示例：智能客服路由

```java
@Component
public class CustomerServiceRoutingWorkflow {
    
    private final ChatClient chatClient;
    private final Map<String, String> routePrompts;
    
    public String route(String userQuery) {
        String routeResult = chatClient.prompt()
                .user("判断用户问题属于哪个类别：技术问题/账户问题/内容问题。只返回类别名称。\n\n问题：" + userQuery)
                .call()
                .content();
        
        String prompt = routePrompts.getOrDefault(routeResult, routePrompts.get("default"));
        
        return chatClient.prompt()
                .system(prompt)
                .user(userQuery)
                .call()
                .content();
    }
}
```

**技术亮点**：
- 根据业务场景选择最合适的工作流模式
- 支持复杂任务的自动化处理
- 可扩展性强，易于添加新的工作流

---

### 3.5 RAG 知识问答系统

#### 3.5.1 架构设计

```
用户提问 → 向量检索 → 构建上下文 → LLM生成回答 → 返回答案+来源
    ↓           ↓            ↓              ↓
  Query    pgvector     Prompt         Response
```

#### 3.5.2 向量存储实现

使用 PostgreSQL + pgvector 扩展实现向量存储：

```java
@Configuration
public class VectorStoreConfig {
    
    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(DistanceType.COSINE_DISTANCE)
                .indexType(IndexType.HNSW)
                .build();
    }
}
```

#### 3.5.3 文章向量化同步

文章发布时自动同步到向量数据库：

```java
@Component
public class ArticleVectorSyncHandler {
    
    private final VectorStore vectorStore;
    
    @RabbitListener(queues = "article.vector.sync")
    public void handleArticleSync(ArticleSyncEvent event) {
        Document document = new Document(
            event.getArticleId().toString(),
            event.getContent(),
            Map.of(
                "title", event.getTitle(),
                "authorId", event.getAuthorId().toString()
            )
        );
        vectorStore.add(List.of(document));
    }
}
```

#### 3.5.4 RAG 检索实现

```java
@Service
public class RAGService {
    
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    
    public String query(String question) {
        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.query(question)
                .withTopK(5)
                .withSimilarityThreshold(0.7)
        );
        
        String context = documents.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));
        
        return chatClient.prompt()
                .system("""
                    你是一个知识问答助手。基于以下上下文回答用户问题。
                    如果上下文中没有相关信息，请说明无法回答。
                    
                    上下文：
                    """ + context)
                .user(question)
                .call()
                .content();
    }
}
```

**技术亮点**：
- 使用 pgvector 扩展，无需额外部署向量数据库
- 支持文章实时向量化同步
- 支持相似度检索和混合检索

---

### 3.6 全文检索与智能推荐

#### 3.6.1 Elasticsearch 集成

```java
@Service
public class SearchService {
    
    private final ElasticsearchClient esClient;
    
    public SearchResultVO search(String keyword, String type, int current, int size) {
        SearchResponse<ArticleDocument> response = esClient.search(s -> s
                .index("article")
                .query(q -> q
                        .bool(b -> b
                                .should(s1 -> s1.match(m -> m.field("title").query(keyword)))
                                .should(s2 -> s2.match(m -> m.field("content").query(keyword)))
                        )
                )
                .from((current - 1) * size)
                .size(size)
                .highlight(h -> h
                        .fields("title", f -> f.preTags("<em>").postTags("</em>"))
                        .fields("content", f -> f.preTags("<em>").postTags("</em>"))
                ),
                ArticleDocument.class
        );
        
        return buildSearchResult(response);
    }
}
```

#### 3.6.2 搜索建议实现

```java
public SuggestVO suggest(String keyword, int size) {
    SearchResponse<ArticleDocument> response = esClient.search(s -> s
            .index("article")
            .query(q -> q.match(m -> m.field("title").query(keyword)))
            .size(size),
            ArticleDocument.class
    );
    
    return SuggestVO.builder()
            .articles(response.hits().hits().stream()
                    .map(this::toSuggestItem)
                    .toList())
            .build();
}
```

#### 3.6.3 文章索引结构

```java
@Document(indexName = "article")
public class ArticleDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;
    
    @Field(type = FieldType.Keyword)
    private String authorName;
    
    @Field(type = FieldType.Keyword)
    private String categoryName;
    
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Long)
    private Long views;
    
    @Field(type = FieldType.Long)
    private Long likes;
}
```

**技术亮点**：
- 支持全文检索、高亮显示
- 支持搜索建议、自动补全
- 支持多索引联合搜索（文章、问题、用户）
- 使用 IK 分词器支持中文分词

---

### 3.7 异步处理与消息队列

#### 3.7.1 RabbitMQ 消息队列应用

| 场景 | 队列 | 消费者 | 说明 |
|------|------|--------|------|
| 评论通知 | comment.notify | NotificationService | 异步发送评论通知 |
| 文章向量化 | article.vector.sync | VectorSyncHandler | 异步同步文章到向量库 |
| 邮件发送 | email.send | EmailService | 异步发送邮件 |
| 数据统计 | stats.update | StatsService | 异步更新统计数据 |

#### 3.7.2 异步评论通知实现

```java
// 生产者
@Service
public class CommentService {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishComment(Comment comment) {
        save(comment);
        
        CommentNotifyEvent event = new CommentNotifyEvent(
            comment.getArticleId(),
            comment.getAuthorId(),
            comment.getContent()
        );
        rabbitTemplate.convertAndSend("comment.notify", event);
    }
}

// 消费者
@Component
public class CommentNotifyConsumer {
    
    @RabbitListener(queues = "comment.notify")
    public void handleNotify(CommentNotifyEvent event) {
        notificationService.sendCommentNotification(event);
    }
}
```

#### 3.7.3 死信队列配置

```java
@Configuration
public class RabbitMQConfig {
    
    @Bean
    public Queue commentNotifyQueue() {
        return QueueBuilder.durable("comment.notify")
                .deadLetterExchange("dlx.exchange")
                .deadLetterRoutingKey("dlx.comment.notify")
                .build();
    }
    
    @Bean
    public Queue deadLetterQueue() {
        return new Queue("dlx.comment.notify", true);
    }
}
```

**技术亮点**：
- 削峰填谷，提高系统吞吐量
- 解耦业务逻辑，提高可维护性
- 支持消息重试和死信队列

---

### 3.8 分布式任务调度

#### 3.8.1 XXL-Job 集成

```java
@Component
public class ArticleHotTask {
    
    @XxlJob("updateHotArticles")
    public void updateHotArticles() {
        List<Article> hotArticles = calculateHotArticles();
        redisTemplate.opsForValue().set("hot:articles", hotArticles);
    }
    
    @XxlJob("syncArticleToEs")
    public void syncArticleToEs() {
        List<Article> articles = getUnsyncedArticles();
        articleSearchService.batchIndex(articles);
    }
    
    @XxlJob("cleanExpiredData")
    public void cleanExpiredData() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        messageService.deleteOldMessages(threshold);
    }
}
```

**技术亮点**：
- 支持分布式任务调度
- 支持任务失败重试
- 支持动态调整任务执行时间

---

### 3.9 前端技术亮点

#### 3.9.1 Vue 3 组合式 API

```vue
<script setup>
import { ref, onMounted, computed } from 'vue'
import { articleApi } from '@/utils/request'

const articles = ref([])
const loading = ref(false)
const pagination = ref({ current: 1, size: 10, total: 0 })

const fetchArticles = async () => {
  loading.value = true
  try {
    const data = await articleApi.list(pagination.value)
    articles.value = data.records
    pagination.value.total = data.total
  } finally {
    loading.value = false
  }
}

onMounted(fetchArticles)
</script>
```

#### 3.9.2 Long 类型 ID 处理

使用 `json-bigint` 解决 JavaScript 大整数精度丢失问题：

```javascript
import JSONbig from 'json-bigint'

const JSONbigString = JSONbig({ storeAsString: true })

const request = async (url, options) => {
  const response = await fetch(url, options)
  const text = await response.text()
  const data = JSONbigString.parse(text)
  return data.data
}
```

#### 3.9.3 SSE 流式响应处理

```javascript
const sendMessageStream = async (conversationId, content) => {
  const stream = await aiApi.sendMessageStream(conversationId, content)
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    
    for (const part of parts) {
      if (part.startsWith('data:')) {
        const event = JSON.parse(part.substring(5).trim())
        if (event.eventType === 1) {
          assistantMessage.content += event.eventData
        }
      }
    }
  }
}
```

#### 3.9.4 Pinia 状态管理

```javascript
// stores/user.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const isLoggedIn = computed(() => !!userInfo.value)
  
  const setUser = (user) => {
    userInfo.value = user
    localStorage.setItem('userInfo', JSON.stringify(user))
  }
  
  const logout = () => {
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }
  
  return {
    userInfo,
    isLoggedIn,
    setUser,
    logout
  }
})
```

**技术亮点**：
- 采用 Vue 3 组合式 API，代码更清晰
- TypeScript 类型约束，减少运行时错误
- 完善的大整数处理方案
- 流式响应支持打字机效果

---

### 3.10 数据库设计亮点

#### 3.10.1 雪花算法主键

使用雪花算法生成分布式唯一 ID：

```java
@Bean
public IdentifierGenerator identifierGenerator() {
    return new IdentifierGenerator() {
        @Override
        public Number nextId(Object entity) {
            return SnowflakeIdGenerator.nextId();
        }
    };
}
```

#### 3.10.2 逻辑删除设计

```java
@TableLogic
@TableField(value = "is_deleted")
private Boolean isDeleted;
```

#### 3.10.3 索引优化策略

| 表 | 索引类型 | 字段 | 说明 |
|------|----------|------|------|
| tb_article | 联合索引 | (author_id, created_at) | 作者文章列表查询 |
| tb_article | 全文索引 | (title, content) | 全文搜索 |
| tb_comment | 联合索引 | (article_id, created_at) | 文章评论列表 |
| tb_user | 唯一索引 | (username), (email) | 用户名/邮箱唯一 |

#### 3.10.4 核心数据表设计

```sql
-- 文章表
CREATE TABLE tb_article (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content TEXT,
    cover VARCHAR(255),
    author_id BIGINT NOT NULL,
    category_id BIGINT,
    views BIGINT DEFAULT 0,
    likes BIGINT DEFAULT 0,
    comments BIGINT DEFAULT 0,
    collections BIGINT DEFAULT 0,
    status INTEGER DEFAULT 0,
    is_top BOOLEAN DEFAULT FALSE,
    is_hot BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_article_author_created ON tb_article(author_id, created_at DESC);
CREATE INDEX idx_article_category ON tb_article(category_id);
CREATE INDEX idx_article_status ON tb_article(status, is_deleted);
```

**技术亮点**：
- 雪花算法保证分布式 ID 唯一性
- 逻辑删除支持数据恢复
- 合理的索引设计提升查询性能

---

## 四、认证与安全

### 4.1 JWT Token 认证

#### 4.1.1 Token 结构

```
Header: {
  "alg": "HS256",
  "typ": "JWT"
}
Payload: {
  "sub": "userId",
  "username": "xxx",
  "exp": 1234567890,
  "iat": 1234567890
}
Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

#### 4.1.2 认证流程

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  用户   │────>│ 登录接口 │────>│ 生成JWT │────>│ 返回Token│
└─────────┘     └─────────┘     └─────────┘     └─────────┘
                                                      │
                                                      ▼
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  访问   │<────│ 验证通过 │<────│ 校验JWT │<────│ 携带Token│
│  资源   │     └─────────┘     └─────────┘     └─────────┘
└─────────┘
```

#### 4.1.3 Spring Security 配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/article/articles/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

#### 4.1.4 Token 刷新机制

```java
@Service
public class TokenService {
    
    private static final long TOKEN_EXPIRE = 30 * 60 * 1000;
    private static final long REFRESH_THRESHOLD = 10 * 60 * 1000;
    
    public String refreshToken(String token) {
        Claims claims = parseToken(token);
        long expireTime = claims.getExpiration().getTime();
        long currentTime = System.currentTimeMillis();
        
        if (expireTime - currentTime < REFRESH_THRESHOLD) {
            return generateToken(claims.getSubject());
        }
        return token;
    }
}
```

### 4.2 权限控制

| 操作 | 权限要求 |
|------|---------|
| 浏览文章/问题/回答 | 公开 |
| 发布文章/问题/回答 | 需登录 |
| 删除文章 | 仅作者本人 |
| 删除评论 | 仅评论作者 |
| 采纳最佳答案 | 仅问题作者 |

### 4.3 安全防护措施

#### 4.3.1 XSS 防护

```java
@Component
public class XssFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) {
        XssHttpServletRequestWrapper wrappedRequest = 
            new XssHttpServletRequestWrapper((HttpServletRequest) request);
        chain.doFilter(wrappedRequest, response);
    }
}
```

#### 4.3.2 SQL 注入防护

使用 MyBatis Plus 的参数化查询，避免 SQL 注入：

```java
// 安全的查询方式
LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Article::getAuthorId, userId);
List<Article> articles = articleMapper.selectList(wrapper);
```

#### 4.3.3 接口限流

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) {
        String key = "rate_limit:" + getClientIp(request);
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        
        if (count > 100) {
            response.setStatus(429);
            return false;
        }
        return true;
    }
}
```

---

## 五、缓存策略

### 5.1 Redis 缓存架构

```
┌─────────────────────────────────────────────────────────────┐
│                        应用层                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  用户缓存   │  │  文章缓存   │  │  会话缓存   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        Redis 集群                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Master    │  │   Master    │  │   Master    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│        │                │                │                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │    Slave    │  │    Slave    │  │    Slave    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 缓存策略设计

| 数据类型 | 缓存策略 | 过期时间 | 说明 |
|---------|---------|---------|------|
| 用户信息 | Cache-Aside | 30分钟 | 读多写少 |
| 文章详情 | Cache-Aside | 1小时 | 读多写少 |
| 热门文章 | Write-Through | 10分钟 | 定时刷新 |
| 点赞数 | Write-Behind | 5分钟 | 高频写入 |
| 会话信息 | TTL | 30分钟 | 自动过期 |

### 5.3 缓存实现

```java
@Service
public class ArticleCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String ARTICLE_CACHE_KEY = "article:detail:";
    
    @Cacheable(value = "article", key = "#id")
    public ArticleDetailVO getArticleDetail(Long id) {
        String key = ARTICLE_CACHE_KEY + id;
        ArticleDetailVO cached = (ArticleDetailVO) redisTemplate.opsForValue().get(key);
        
        if (cached != null) {
            return cached;
        }
        
        ArticleDetailVO article = articleService.getDetailById(id);
        redisTemplate.opsForValue().set(key, article, 1, TimeUnit.HOURS);
        
        return article;
    }
    
    @CacheEvict(value = "article", key = "#id")
    public void evictCache(Long id) {
        String key = ARTICLE_CACHE_KEY + id;
        redisTemplate.delete(key);
    }
}
```

### 5.4 缓存穿透防护

```java
public ArticleDetailVO getArticleDetailWithBloomFilter(Long id) {
    if (!bloomFilter.mightContain(id)) {
        return null;
    }
    
    String key = ARTICLE_CACHE_KEY + id;
    ArticleDetailVO cached = (ArticleDetailVO) redisTemplate.opsForValue().get(key);
    
    if (cached != null) {
        return cached;
    }
    
    ArticleDetailVO article = articleService.getDetailById(id);
    
    if (article == null) {
        redisTemplate.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);
        return null;
    }
    
    redisTemplate.opsForValue().set(key, article, 1, TimeUnit.HOURS);
    return article;
}
```

---

## 六、性能优化

### 6.1 数据库优化

#### 6.1.1 慢查询优化

```sql
-- 开启慢查询日志
SET slow_query_log = ON;
SET long_query_time = 2;

-- 分析执行计划
EXPLAIN ANALYZE SELECT * FROM tb_article WHERE author_id = 123 ORDER BY created_at DESC;
```

#### 6.1.2 连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 30000
      max-lifetime: 1200000
```

### 6.2 接口性能优化

#### 6.2.1 批量查询优化

```java
// 优化前：N+1 查询
List<Article> articles = articleMapper.selectList(wrapper);
for (Article article : articles) {
    User author = userMapper.selectById(article.getAuthorId());
    article.setAuthorName(author.getName());
}

// 优化后：批量查询
List<Article> articles = articleMapper.selectList(wrapper);
Set<Long> authorIds = articles.stream()
        .map(Article::getAuthorId)
        .collect(Collectors.toSet());
Map<Long, User> authorMap = userMapper.selectBatchIds(authorIds)
        .stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
articles.forEach(article -> 
    article.setAuthorName(authorMap.get(article.getAuthorId()).getName()));
```

#### 6.2.2 异步并行查询

```java
public ArticleDetailVO getArticleDetail(Long id) {
    CompletableFuture<Article> articleFuture = CompletableFuture.supplyAsync(
        () -> articleMapper.selectById(id));
    
    CompletableFuture<List<Comment>> commentsFuture = CompletableFuture.supplyAsync(
        () -> commentMapper.selectByArticleId(id));
    
    CompletableFuture<ArticleStats> statsFuture = CompletableFuture.supplyAsync(
        () -> statsMapper.selectByArticleId(id));
    
    CompletableFuture.allOf(articleFuture, commentsFuture, statsFuture).join();
    
    return buildArticleDetailVO(
        articleFuture.get(), 
        commentsFuture.get(), 
        statsFuture.get()
    );
}
```

### 6.3 前端性能优化

#### 6.3.1 路由懒加载

```javascript
const routes = [
  {
    path: '/article/:id',
    component: () => import('@/views/ArticleDetail.vue')
  },
  {
    path: '/ai-chat',
    component: () => import('@/views/AIChat.vue')
  }
]
```

#### 6.3.2 图片懒加载

```vue
<template>
  <img v-lazy="imageUrl" alt="article cover" />
</template>

<script setup>
import { useIntersectionObserver } from '@vueuse/core'

const vLazy = {
  mounted(el, binding) {
    const { stop } = useIntersectionObserver(el, ([{ isIntersecting }]) => {
      if (isIntersecting) {
        el.src = binding.value
        stop()
      }
    })
  }
}
</script>
```

---

## 七、监控与日志

### 7.1 日志规范

```java
@Slf4j
@Service
public class ArticleService {
    
    public void publishArticle(ArticlePublishDTO dto) {
        log.info("发布文章开始, userId: {}, title: {}", 
            UserContextHolder.getUserId(), dto.getTitle());
        
        try {
            // 业务逻辑
            log.info("文章发布成功, articleId: {}", article.getId());
        } catch (Exception e) {
            log.error("文章发布失败, userId: {}, error: {}", 
                UserContextHolder.getUserId(), e.getMessage(), e);
            throw e;
        }
    }
}
```

### 7.2 接口监控

```java
@Aspect
@Component
public class ApiMonitorAspect {
    
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("接口调用成功: {}, 耗时: {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("接口调用失败: {}, 耗时: {}ms, 错误: {}", 
                methodName, duration, e.getMessage());
            throw e;
        }
    }
}
```

---

## 八、部署架构

### 8.1 生产环境架构

```
┌─────────────────────────────────────────────────────────────────┐
│                          Nginx 负载均衡                          │
└─────────────────────────────────────────────────────────────────┘
                    │                           │
        ┌───────────┴───────────┐   ┌──────────┴──────────┐
        ▼                       ▼   ▼                     ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│  Spring Boot  │       │  Spring Boot  │       │  Python AI    │
│  Instance 1   │       │  Instance 2   │       │  Service      │
└───────────────┘       └───────────────┘       └───────────────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│  PostgreSQL   │       │    Redis      │       │ Elasticsearch │
│   Primary     │       │    Cluster    │       │    Cluster    │
└───────────────┘       └───────────────┘       └───────────────┘
```

### 8.2 Docker 部署

```yaml
# docker-compose.yml
version: '3.8'
services:
  backend:
    build: ./project-backen
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - postgres
      - redis
  
  frontend:
    build: ./project-front
    ports:
      - "3000:80"
    depends_on:
      - backend
  
  ai-service:
    build: ./project-ai-agent
    ports:
      - "8000:8000"
    depends_on:
      - postgres
  
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: blog
      POSTGRES_USER: blog
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
  
  redis:
    image: redis:7.2
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

---

## 九、开发规范

### 9.1 Java 编码规范

- 遵循阿里巴巴 Java 开发规范
- 采用 RESTful API 设计风格
- 包名：`com.personblog.{模块名}`
- 类名：大驼峰命名，如 `ArticleController`
- 方法名：小驼峰命名，如 `getArticleById`
- Long 类型 ID 必须添加 `@JsonSerialize(using = ToStringSerializer.class)` 注解

### 9.2 三层架构规范

- Controller 层：处理 HTTP 请求，参数校验，调用 Service
- Service 层：业务逻辑处理，事务管理
- Mapper 层：数据库访问

### 9.3 前端编码规范

- 采用 Vue 3 组合式 API
- 采用 TypeScript 类型约束
- 组件名：大驼峰命名
- 变量/方法：小驼峰命名
- 使用 `json-bigint` 处理 Long 类型 ID

---

## 十、常见问题与解决方案

### 10.1 Long 类型 ID 精度丢失

**问题**：JavaScript Number 类型最大安全整数为 2^53 - 1，后端雪花算法生成的 Long 类型 ID 会超过此范围。

**解决方案**：
- 后端：VO/DTO 类中的 Long 类型 ID 字段添加 `@JsonSerialize(using = ToStringSerializer.class)` 注解
- 前端：使用 `json-bigint` 库处理，配置 `{ storeAsString: true }`

### 10.2 SSE 流式响应处理

**问题**：AI 对话使用 SSE 流式响应，需要正确解析。

**解决方案**：使用 Fetch API + ReadableStream 处理 SSE 数据流

### 10.3 跨模块调用

**问题**：模块间存在循环依赖。

**解决方案**：
- 通过 `blog-api` 模块定义 API 接口
- 服务提供方实现 API 接口
- 服务消费方依赖 `blog-api` 模块，注入 API 接口使用

### 10.4 缓存一致性问题

**问题**：数据库更新后缓存未同步。

**解决方案**：
- 使用 Cache-Aside 模式，先更新数据库，再删除缓存
- 使用消息队列异步更新缓存
- 设置合理的缓存过期时间

---

## 十一、业务模块详解

### 11.1 文章管理模块

#### 11.1.1 文章发布流程

```
用户提交文章
    ↓
参数校验（标题、内容、分类、标签）
    ↓
保存文章到数据库
    ↓
异步处理：
├── 更新用户文章计数
├── 同步文章到 Elasticsearch
├── 更新标签使用次数
├── 更新分类文章计数
├── 清除相关缓存
└── 发送 AI 评论生成消息
```

#### 11.1.2 文章列表查询优化

```java
// 批量查询优化，避免 N+1 问题
private List<ArticleListVO> convertToVOList(List<Article> articles) {
    // 1. 收集所有作者ID
    Set<Long> authorIds = articles.stream()
            .map(Article::getAuthorId)
            .collect(Collectors.toSet());
    
    // 2. 批量查询用户信息
    Map<Long, UserDTO> userMap = useApi.getUserInfo(authorIds)
            .stream()
            .collect(Collectors.toMap(UserDTO::getId, u -> u));
    
    // 3. 批量从 Redis 获取浏览量增量
    List<Object> browseCounts = redisTemplate.opsForHash()
            .multiGet(BROWSE_COUNT_KEY, articleIdStrs);
    
    // 4. 组装 VO 对象
    return articles.stream().map(article -> {
        ArticleListVO vo = new ArticleListVO();
        // 设置作者信息、分类、标签、浏览量等
        return vo;
    }).collect(Collectors.toList());
}
```

#### 11.1.3 文章缓存策略

| 数据 | 缓存位置 | TTL | 更新策略 |
|------|---------|-----|---------|
| Banner 轮播图 | Caffeine | 1小时 | 定时刷新 |
| 热门文章 | Caffeine | 5分钟 | 定时刷新 |
| 文章详情 | Redis + Caffeine | 10分钟 + 2分钟 | 写时失效 |
| 用户信息 | Redis + Caffeine | 10分钟 + 5分钟 | 写时失效 |

---

### 11.2 评论系统模块

#### 11.2.1 评论数据结构

```
评论（Comment）
├── id: 评论ID
├── articleId: 文章ID
├── authorId: 评论作者ID
├── parentId: 父评论ID（为空表示一级评论）
├── content: 评论内容
├── likes: 点赞数
├── status: 状态（待审核/已通过/已拒绝）
└── createdAt: 创建时间
```

#### 11.2.2 评论树形结构查询

```java
// 获取评论及回复
public Page<CommentVO> getCommentPage(Long articleId, Integer current, Integer size) {
    // 1. 查询一级评论
    List<Comment> comments = lambdaQuery()
            .eq(Comment::getArticleId, articleId)
            .isNull(Comment::getParentId)
            .eq(Comment::getStatus, APPROVED)
            .orderByDesc(Comment::getCreatedAt)
            .list();
    
    // 2. 批量查询所有回复
    List<Long> commentIds = comments.stream()
            .map(Comment::getId)
            .toList();
    Map<Long, List<Comment>> repliesMap = getRepliesMap(commentIds);
    
    // 3. 组装树形结构
    return comments.stream().map(comment -> {
        CommentVO vo = convertToVO(comment);
        vo.setReplies(convertRepliesToVO(repliesMap.get(comment.getId())));
        return vo;
    }).toList();
}
```

#### 11.2.3 评论通知机制

```java
// 评论创建后异步发送通知
CompletableFuture.runAsync(() -> {
    // 确定通知接收者
    Long receiverId;
    if (parentId != null) {
        // 回复评论：通知被回复的评论作者
        receiverId = parentComment.getAuthorId();
        actionType = "reply";
    } else {
        // 评论文章：通知文章作者
        receiverId = authorId;
        actionType = "comment";
    }
    
    // 保存通知到数据库
    Long notificationId = notificationApi.saveNotification(messageDTO);
    
    // SSE 实时推送
    sseEmitterManager.sendToUser(receiverId, messageDTO);
}, executor);
```

---

### 11.3 用户互动模块

#### 11.3.1 关注系统

```java
// 关注/取关操作
public FollowVO doFollow(FollowDTO dto) {
    boolean isSuccess = dto.getIsFollow() 
            ? unFollow(dto.getFollowingId(), userId) 
            : followed(dto.getFollowingId(), userId);
    
    // 发送消息到消息队列
    FollowMessageDTO messageDTO = FollowMessageDTO.builder()
            .followingId(dto.getFollowingId())
            .isFollow(dto.getIsFollow())
            .followerId(userId)
            .build();
    
    // 1. 更新粉丝数和关注数
    rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, FOLLOW_KEY, messageDTO);
    
    // 2. 发送关注通知
    if (!dto.getIsFollow()) {
        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, FOLLOW_NOTIFICATION_KEY, messageDTO);
    }
    
    return FollowVO.builder().isFollow(!dto.getIsFollow()).build();
}
```

#### 11.3.2 点赞系统设计

```
点赞流程：
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  用户点赞   │────>│ Redis 计数  │────>│ 返回成功    │
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ 定时同步DB  │
                    └─────────────┘
```

#### 11.3.3 收藏系统

```java
// 收藏文章
public void collectArticle(Long articleId, Long userId) {
    // 1. 保存收藏记录
    Collection collection = new Collection();
    collection.setArticleId(articleId);
    collection.setUserId(userId);
    save(collection);
    
    // 2. 更新文章收藏数（Redis ZSet）
    redisTemplate.opsForZSet().incrementScore(COLLECTION_TIMES_KEY_PREFIX, articleId.toString(), 1);
    
    // 3. 异步同步到数据库
    rabbitTemplate.convertAndSend(COLLECTION_EXCHANGE, COLLECTION_KEY, messageDTO);
}
```

---

### 11.4 问答社区模块

#### 11.4.1 问题数据结构

```sql
CREATE TABLE tb_question (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    author_id BIGINT NOT NULL,
    views BIGINT DEFAULT 0,
    answers INT DEFAULT 0,
    likes BIGINT DEFAULT 0,
    is_solved BOOLEAN DEFAULT FALSE,
    best_answer_id BIGINT,
    status INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tb_answer (
    id BIGINT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    content TEXT,
    author_id BIGINT NOT NULL,
    likes BIGINT DEFAULT 0,
    is_accepted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 11.4.2 采纳最佳答案

```java
// 采纳最佳答案
public void acceptAnswer(Long questionId, Long answerId, Long userId) {
    // 1. 校验权限（只有问题作者可以采纳）
    Question question = getById(questionId);
    if (!question.getAuthorId().equals(userId)) {
        throw new BizException(NO_POWER);
    }
    
    // 2. 更新答案状态
    answerService.lambdaUpdate()
            .eq(Answer::getId, answerId)
            .set(Answer::getIsAccepted, true)
            .update();
    
    // 3. 更新问题状态
    lambdaUpdate()
            .eq(Question::getId, questionId)
            .set(Question::getIsSolved, true)
            .set(Question::getBestAnswerId, answerId)
            .update();
    
    // 4. 发送通知给回答者
    notificationService.sendAcceptNotification(answerId);
}
```

---

## 十二、数据同步机制

### 12.1 Elasticsearch 数据同步

#### 12.1.1 同步策略

| 场景 | 同步方式 | 说明 |
|------|---------|------|
| 文章发布/更新 | 实时同步 | 通过 API 调用立即同步 |
| 批量导入 | 定时任务 | XXL-Job 定时全量同步 |
| 文章删除 | 实时删除 | 同步删除 ES 文档 |

#### 12.1.2 同步实现

```java
// 文章发布时同步到 ES
CompletableFuture.runAsync(() -> 
    searchSyncApi.syncArticle(articleId), articleCountExecutor
).exceptionally(e -> {
    log.error("同步文章到ES失败, articleId={}", articleId, e);
    return null;
});

// 同步服务实现
public void syncArticle(Long articleId) {
    Article article = articleService.getById(articleId);
    ArticleDocument document = convertToDocument(article);
    elasticsearchOperations.save(document);
}
```

### 12.2 Redis 数据同步

#### 12.2.1 点赞数同步

```java
// 定时任务：同步 Redis 点赞数到数据库
@XxlJob("syncLikeCount")
public void syncLikeCount() {
    // 1. 从 Redis 获取所有点赞计数
    Map<Object, Object> likeCounts = redisTemplate.opsForHash().entries(LIKE_COUNT_KEY);
    
    // 2. 批量更新数据库
    List<LikeMessageDTO> dtoList = likeCounts.entrySet().stream()
            .map(e -> LikeMessageDTO.builder()
                    .id(Long.parseLong(e.getKey().toString()))
                    .likeTimes(Long.parseLong(e.getValue().toString()))
                    .build())
            .toList();
    
    articleService.updateLikeCount(dtoList);
    
    // 3. 清空 Redis 计数
    redisTemplate.delete(LIKE_COUNT_KEY);
}
```

#### 12.2.2 浏览量同步

```java
// 浏览量增量存储在 Redis Hash
public void incrementBrowseCount(Long articleId) {
    redisTemplate.opsForHash().increment(BROWSE_COUNT_KEY, articleId.toString(), 1);
}

// 定时同步到数据库
@XxlJob("syncBrowseCount")
public void syncBrowseCount() {
    Map<Object, Object> browseCounts = redisTemplate.opsForHash().entries(BROWSE_COUNT_KEY);
    List<BrowseHistoryMessageDTO> dtoList = buildSyncList(browseCounts);
    articleService.updateBrowseCount(dtoList);
    redisTemplate.delete(BROWSE_COUNT_KEY);
}
```

---

## 十三、SSE 实时推送

### 13.1 SSE 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        前端客户端                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  EventSource / fetch SSE                            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      后端服务                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ SseEmitter  │  │ SseEmitter  │  │ SseEmitter  │         │
│  │  Manager    │  │  Manager    │  │  Manager    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│         │                │                │                 │
│         └────────────────┼────────────────┘                 │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           ConcurrentHashMap<userId, SseEmitter>     │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 13.2 SseEmitterManager 实现

```java
@Component
public class SseEmitterManager {
    
    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    
    // 创建 SSE 连接
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(0L); // 永不超时
        
        emitter.onCompletion(() -> emitterMap.remove(userId));
        emitter.onTimeout(() -> emitterMap.remove(userId));
        emitter.onError(e -> emitterMap.remove(userId));
        
        emitterMap.put(userId, emitter);
        return emitter;
    }
    
    // 发送消息给指定用户
    public void sendToUser(Long userId, Object message) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .data(JSON.toJSONString(message))
                        .name("message"));
            } catch (IOException e) {
                emitterMap.remove(userId);
            }
        }
    }
    
    // 广播消息给所有用户
    public void broadcast(Object message) {
        emitterMap.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (IOException e) {
                emitterMap.remove(userId);
            }
        });
    }
}
```

### 13.3 SSE 控制器

```java
@RestController
@RequestMapping("/api/sse")
public class SseController {
    
    private final SseEmitterManager sseEmitterManager;
    
    // 建立 SSE 连接
    @GetMapping("/connect")
    public SseEmitter connect() {
        Long userId = UserContextHolder.getUserId();
        return sseEmitterManager.createEmitter(userId);
    }
    
    // 发送心跳
    @GetMapping("/heartbeat")
    public void heartbeat() {
        Long userId = UserContextHolder.getUserId();
        sseEmitterManager.sendToUser(userId, "ping");
    }
}
```

### 13.4 前端 SSE 连接

```javascript
// 建立 SSE 连接
const connectSSE = () => {
  const eventSource = new EventSource('/api/sse/connect')
  
  eventSource.addEventListener('message', (event) => {
    const notification = JSON.parse(event.data)
    // 处理通知
    showNotification(notification)
  })
  
  eventSource.onerror = (error) => {
    console.error('SSE connection error:', error)
    // 重连逻辑
    setTimeout(connectSSE, 5000)
  }
  
  return eventSource
}
```

---

## 十四、多级缓存架构

### 14.1 缓存架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        应用层                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   请求处理                           │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    L1: 本地缓存 (Caffeine)                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  用户缓存   │  │  文章缓存   │  │  Banner缓存 │         │
│  │  TTL: 5min  │  │  TTL: 2min  │  │  TTL: 1h    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │ Miss
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    L2: 分布式缓存 (Redis)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  用户信息   │  │  文章详情   │  │  会话信息   │         │
│  │  TTL: 10min │  │  TTL: 10min │  │  TTL: 30min │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │ Miss
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    L3: 数据库 (PostgreSQL)                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   数据持久化                         │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 14.2 MultiLevelCacheUtil 实现

```java
@Component
public class MultiLevelCacheUtil {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 本地缓存容器
    private final Map<String, Cache<String, Object>> localCaches = new ConcurrentHashMap<>();
    
    /**
     * 获取缓存数据
     * @param prefix Redis key 前缀
     * @param key 缓存 key
     * @param loader 数据加载函数
     * @param redisTtl Redis 过期时间（秒）
     * @param localTtl 本地缓存过期时间（秒）
     */
    public <T> T get(String prefix, String key, 
                     Function<String, T> loader, 
                     int redisTtl, int localTtl, 
                     Class<T> clazz) {
        String fullKey = prefix + key;
        
        // 1. 先查本地缓存
        Cache<String, Object> localCache = getOrCreateLocalCache(key, localTtl);
        Object cached = localCache.getIfPresent(key);
        if (cached != null) {
            return clazz.cast(cached);
        }
        
        // 2. 再查 Redis
        Object redisValue = redisTemplate.opsForValue().get(fullKey);
        if (redisValue != null) {
            localCache.put(key, redisValue);
            return clazz.cast(redisValue);
        }
        
        // 3. 查数据库
        T value = loader.apply(key);
        if (value != null) {
            // 写入 Redis
            redisTemplate.opsForValue().set(fullKey, value, redisTtl, TimeUnit.SECONDS);
            // 写入本地缓存
            localCache.put(key, value);
        }
        
        return value;
    }
    
    // 清除缓存
    public void evict(String prefix, String key) {
        String fullKey = prefix + key;
        redisTemplate.delete(fullKey);
        getLocalCache(key).invalidate(key);
    }
}
```

### 14.3 缓存使用示例

```java
// 获取文章详情
public ArticleDetailVO getArticleDetail(Long id) {
    String cacheKey = ARTICLE_DETAIL + id;
    
    return cacheUtil.get(
            "",  // Redis Key 前缀已在 cacheKey 中
            cacheKey,
            key -> loadArticleDetailFromDB(id),  // 数据库加载函数
            600,  // Redis TTL: 10分钟
            120,  // 本地缓存 TTL: 2分钟
            ArticleDetailVO.class
    );
}

// 更新文章后清除缓存
public void updateArticle(Article article) {
    updateById(article);
    cacheUtil.evict("", ARTICLE_DETAIL + article.getId());
    hotArticleCache.invalidateAll();
}
```

---

## 十五、异步任务处理

### 15.1 线程池配置

```java
@Configuration
public class ThreadPoolConfig {
    
    // 文章计数线程池
    @Bean("ArticleCountExecutor")
    public Executor articleCountExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("article-count-");
        executor.initialize();
        return executor;
    }
    
    // 评论处理线程池
    @Bean("CommentExecutor")
    public Executor commentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("comment-");
        executor.initialize();
        return executor;
    }
    
    // 标签处理线程池
    @Bean("TagExecutor")
    public Executor tagExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("tag-");
        executor.initialize();
        return executor;
    }
}
```

### 15.2 异步任务使用

```java
// 文章发布后的异步处理
@Transactional
public ArticlePublishVO createArticle(Long userId, ArticlePublishDTO dto) {
    // 1. 保存文章
    save(article);
    
    // 2. 异步更新用户文章数
    CompletableFuture.runAsync(() -> 
        useApi.updateArticlesCount(userId, 1), articleCountExecutor
    ).exceptionally(e -> {
        log.error("更新用户文章数失败", e);
        return null;
    });
    
    // 3. 异步同步到 ES
    CompletableFuture.runAsync(() -> 
        searchSyncApi.syncArticle(articleId), articleCountExecutor
    ).exceptionally(e -> {
        log.error("同步文章到ES失败", e);
        return null;
    });
    
    // 4. 异步更新标签使用次数
    CompletableFuture.runAsync(() -> 
        updateTagUseCount(tagIds, 1), tagExecutor
    ).exceptionally(e -> {
        log.error("更新标签使用次数失败", e);
        return null;
    });
    
    return vo;
}
```

### 15.3 @Async 异步方法

```java
// 使用 @Async 注解实现异步
@Async("ArticleCountExecutor")
public void updateCommentCount(Long articleId, int delta) {
    lambdaUpdate()
            .eq(Article::getId, articleId)
            .setSql("comments = comments + " + delta)
            .update();
}
```

---

## 十六、消息队列应用

### 16.1 RabbitMQ 配置

```java
@Configuration
public class InteractionMqConfig {
    
    public static final String INTERACTION_EXCHANGE = "interaction.exchange";
    public static final String FOLLOW_KEY = "follow.update";
    public static final String FOLLOW_NOTIFICATION_KEY = "follow.notification";
    public static final String USER_LIKE_KEY = "user.like.update";
    
    @Bean
    public DirectExchange interactionExchange() {
        return new DirectExchange(INTERACTION_EXCHANGE);
    }
    
    @Bean
    public Queue followQueue() {
        return QueueBuilder.durable("follow.update")
                .deadLetterExchange("dlx.exchange")
                .build();
    }
    
    @Bean
    public Binding followBinding() {
        return BindingBuilder.bind(followQueue())
                .to(interactionExchange())
                .with(FOLLOW_KEY);
    }
}
```

### 16.2 消息生产者

```java
// 关注操作发送消息
public FollowVO doFollow(FollowDTO dto) {
    // 业务逻辑...
    
    // 发送消息到队列
    FollowMessageDTO messageDTO = FollowMessageDTO.builder()
            .followingId(dto.getFollowingId())
            .isFollow(dto.getIsFollow())
            .followerId(userId)
            .build();
    
    rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, FOLLOW_KEY, messageDTO);
    
    return vo;
}
```

### 16.3 消息消费者

```java
@Component
public class FollowConsumer {
    
    private final UseApi useApi;
    
    @RabbitListener(queues = "follow.update")
    public void handleFollowUpdate(FollowMessageDTO message) {
        if (message.getIsFollow()) {
            // 取关：减少粉丝数和关注数
            useApi.updateFanCount(message.getFollowingId(), -1);
            useApi.updateFollowingCount(message.getFollowerId(), -1);
        } else {
            // 关注：增加粉丝数和关注数
            useApi.updateFanCount(message.getFollowingId(), 1);
            useApi.updateFollowingCount(message.getFollowerId(), 1);
        }
    }
}
```

### 16.4 消息队列应用场景

| 场景 | 交换机 | 路由键 | 消费者处理 |
|------|--------|--------|-----------|
| 关注/取关 | interaction.exchange | follow.update | 更新粉丝数、关注数 |
| 关注通知 | interaction.exchange | follow.notification | 发送关注通知 |
| 点赞同步 | interaction.exchange | user.like.update | 批量更新用户获赞数 |
| 评论通知 | comment.exchange | comment.notify | 发送评论通知 |
| AI 评论生成 | comment.exchange | ai.comment | 调用 AI 生成评论 |

---

## 十七、测试规范

### 17.1 单元测试

```java
@SpringBootTest
class ArticleServiceTest {
    
    @Autowired
    private IArticleService articleService;
    
    @MockBean
    private UseApi useApi;
    
    @Test
    void testCreateArticle() {
        // Given
        ArticlePublishDTO dto = new ArticlePublishDTO();
        dto.setTitle("测试文章");
        dto.setContent("测试内容");
        dto.setStatus(1);
        
        when(useApi.getUserInfo(any())).thenReturn(List.of(mockUser));
        
        // When
        ArticlePublishVO result = articleService.createArticle(1L, dto);
        
        // Then
        assertNotNull(result.getId());
        assertEquals(1, result.getStatus());
    }
}
```

### 17.2 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArticleControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testGetArticleDetail() {
        // When
        ResponseEntity<ArticleDetailVO> response = restTemplate.getForEntity(
                "/api/article/articles/1", ArticleDetailVO.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getTitle());
    }
}
```

### 17.3 测试覆盖率要求

| 模块 | 最低覆盖率 |
|------|-----------|
| blog-common | 80% |
| blog-security | 85% |
| blog-article | 80% |
| blog-comment | 80% |
| blog-interaction | 75% |
| blog-ai | 70% |

---

## 十八、最佳实践总结

### 18.1 代码规范

1. **命名规范**
   - 类名：大驼峰，如 `ArticleService`
   - 方法名：小驼峰，如 `getArticleById`
   - 常量：全大写下划线，如 `MAX_PAGE_SIZE`
   - 包名：小写，如 `com.personblog.article`

2. **注释规范**
   - 关键业务逻辑必须添加注释
   - 复杂算法必须说明思路
   - 使用中文注释

3. **异常处理**
   - 使用自定义业务异常 `BizException`
   - 统一异常处理返回标准格式
   - 记录详细的错误日志

### 18.2 性能优化实践

1. **数据库优化**
   - 避免 N+1 查询，使用批量查询
   - 合理使用索引
   - 复杂 SQL 写入 XML 文件

2. **缓存优化**
   - 热点数据使用多级缓存
   - 合理设置过期时间
   - 写时失效策略

3. **异步处理**
   - 耗时操作异步执行
   - 使用合适的线程池
   - 做好异常处理

### 18.3 安全实践

1. **认证授权**
   - 使用 JWT Token 认证
   - 敏感操作校验权限
   - Token 自动续期

2. **数据安全**
   - 密码加密存储
   - 敏感数据脱敏
   - SQL 注入防护

3. **接口安全**
   - 接口限流
   - XSS 过滤
   - CSRF 防护

---

## 十九、运维部署

### 19.1 环境配置

| 环境 | 配置文件 | 说明 |
|------|---------|------|
| 开发环境 | application-dev.yml | 本地开发配置 |
| 测试环境 | application-test.yml | 测试环境配置 |
| 生产环境 | application-prod.yml | 生产环境配置 |

### 19.2 Docker 部署

```yaml
# docker-compose.yml
version: '3.8'
services:
  backend:
    build: ./project-backen
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms512m -Xmx1024m
    depends_on:
      - postgres
      - redis
  
  frontend:
    build: ./project-front
    ports:
      - "3000:80"
    depends_on:
      - backend
  
  ai-service:
    build: ./project-ai-agent
    ports:
      - "8000:8000"
    environment:
      - PYTHON_ENV=production
    depends_on:
      - postgres
  
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: blog
      POSTGRES_USER: blog
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
  
  redis:
    image: redis:7.2
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

### 19.3 健康检查

```java
@RestController
public class HealthController {
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        return health;
    }
}
```

---

> **文档版本**: v4.0  
> **更新日期**: 2026-04-18  
> **维护人**: LSH
