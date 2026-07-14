# 项目架构规范

## 后端模块结构

### 模块职责划分

| 模块 | 路径 | 职责 | 依赖 |
|------|------|------|------|
| blog-application | `project-backen/blog-application/` | 启动入口、全局配置、环境 profile | 所有业务模块 |
| blog-common | `project-backen/blog-common/` | 工具库、共享 DTO/VO、MQ 配置、缓存工具、异常处理 | 无（基础层） |
| blog-api | `project-backen/blog-api/` | 跨模块 Feign 接口契约（纯接口） | 无（契约层） |
| blog-security | `project-backen/blog-security/` | JWT 认证、用户管理、Spring Security 配置 | blog-common, blog-api |
| blog-article | `project-backen/blog-article/` | 文章 CRUD、分类、专栏 | blog-common, blog-api |
| blog-comment | `project-backen/blog-comment/` | 评论 CRUD | blog-common, blog-api |
| blog-interaction | `project-backen/blog-interaction/` | 点赞、收藏、关注、私信、通知、浏览历史 | blog-common, blog-api, blog-push |
| blog-question | `project-backen/blog-question/` | 问答 CRUD | blog-common, blog-api |
| blog-ai | `project-backen/blog-ai/` | AI 聊天、摘要、审核、写作 Agent | blog-common, blog-api, blog-push |
| blog-search | `project-backen/blog-search/` | ES 搜索、索引同步、搜索建议 | blog-common, blog-api |
| blog-job | `project-backen/blog-job/` | XXL-Job 定时任务 | blog-common, blog-api |
| blog-push | `project-backen/blog-push/` | WebSocket + SSE 实时推送通道 | blog-common |

### 每个业务模块的标准目录结构

```
blog-{module}/src/main/java/com/personblog/{module}/
├── controller/        # REST 控制器
├── service/           # 服务接口
│   └── impl/          # 服务实现
├── mapper/            # MyBatis Mapper 接口
├── entity/            # 数据库实体
├── dto/               # 请求 DTO（入参）
├── vo/                # 响应 VO（出参）
├── mqHandler/         # RabbitMQ 消息处理器（如适用）
├── bizService/        # 领域业务服务（如策略模式）
└── config/            # 模块特有配置
```

### 模块间依赖规则

1. **禁止循环依赖**：模块间只能单向依赖，通过 `blog-api` 接口解耦
2. **blog-common 是基础层**：被所有模块依赖，不得依赖任何业务模块
3. **blog-api 是契约层**：只定义接口，不包含实现，被所有业务模块依赖
4. **blog-push 是通道层**：只负责推送，不包含业务逻辑
5. **业务模块之间通过 blog-api 接口通信**：不直接 import 其他模块的类

### 跨模块调用模式

```java
// 1. blog-api 定义接口
public interface ArticleInfoAPI {
    ArticleDTO getArticleInfo(Long articleId);
}

// 2. 提供方 ServiceImpl 实现接口
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements IArticleService, ArticleInfoAPI {
    @Override
    public ArticleDTO getArticleInfo(Long articleId) { ... }
}

// 3. 消费方注入接口使用
@Service
@RequiredArgsConstructor
public class CommentServiceImpl {
    private final ArticleInfoAPI articleInfoAPI;  // Spring 注入实现类
}
```

---

## 前端项目结构

### 用户端前端 (project-front)

```
project-front/src/
├── assets/           # 静态资源
├── components/       # 公共组件
├── composables/      # 组合式函数 (useXxx)
├── stores/           # 状态管理 (reactive 模式)
├── utils/            # 工具函数
│   ├── request.js    # API 请求封装 (fetch + json-bigint + Refresh Token)
│   ├── sse.js        # SSE 连接管理器
│   ├── websocket.js  # WebSocket 连接管理器
│   └── ...
└── views/            # 页面组件
```

### 管理端前端 (project-front-admin)

```
project-front-admin/src/
├── assets/           # 静态资源
├── components/       # 公共组件 (AdminLayout, AdminSidebar, AdminHeader)
├── router/           # 路由配置
├── stores/           # Pinia 状态管理
├── utils/            # 工具函数 (request.js 独立于用户端)
└── views/            # 管理页面
```

### 两套前端的差异

| 维度 | 用户端 | 管理端 |
|------|--------|--------|
| UI 框架 | Naive UI | Element Plus |
| 状态管理 | reactive + composable | Pinia |
| localStorage key | `token` / `refresh-token` / `user-info` | `admin-token` / `admin-refresh-token` / `admin-info` |
| API 路径前缀 | `/api/` | `/admin/` |
| 样式方案 | Tailwind CSS | Element Plus 内置 |

---

## Python AI Agent 结构

```
project-ai-agent/
├── main.py           # FastAPI 入口 (create_app 工厂函数)
├── config/           # 配置 (settings.py 使用 Pydantic BaseSettings)
├── api/              # FastAPI 路由 (chat_router, writing_router, knowledge_router)
├── agents/           # LangGraph Agent 实现
├── services/         # 业务逻辑服务
├── tools/            # LangChain 工具
├── models/           # Pydantic 数据模型
└── common/           # 公共组件
```

### Java ↔ Python 通信

- Java 调用 Python：通过 Feign 调用 Python 的 FastAPI 接口
- Python 回调 Java：通过 HTTP 调用 Java 的内部接口（`/internal/` 路径，ApiKeyAuthFilter 验证）
- 服务发现：两者都注册到 Nacos，通过服务名互相调用

---

## 数据库规范

### 表命名
- 表名前缀：`tb_`
- 软删除字段：`is_deleted`（配合 `@TableLogic`）
- 时间字段：`created_at`、`updated_at`
- 雪花 ID：`@TableId(type = IdType.ASSIGN_ID)`

### Entity 模板

```java
@Data
@TableName("tb_article")
public class Article {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String title;
    // ...
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```
