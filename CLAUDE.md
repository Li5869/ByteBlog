# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 项目概览

ByteBlog 是一个前后端分离的博客平台，包含 4 个独立子项目：

| 子项目 | 路径 | 技术栈 | 端口 |
|---|---|---|---|
| 后端 | `project-backen/` | Java 21, Spring Boot 4, MyBatis-Plus, PostgreSQL, Redis, RabbitMQ, Elasticsearch, Nacos | 8080 |
| 用户端前端 | `project-front/` | Vue 3, Tailwind CSS, Naive UI, 原生 fetch（无 Axios） | 5173 |
| 管理端前端 | `project-front-admin/` | Vue 3, Element Plus, Pinia | 5174 |
| AI Agent | `project-ai-agent/` | Python 3.12+, FastAPI, LangGraph, LangChain, pgvector, Mem0 | 8000 |

## 构建与运行命令

### 后端（Maven）
```bash
cd project-backen
mvn clean install                          # 构建所有模块
mvn spring-boot:run -pl blog-application   # 启动应用
mvn test                                   # 运行全部测试
mvn test -pl blog-article                  # 运行单个模块的测试
mvn compile -pl blog-common                # 编译单个模块
```

### 用户端前端
```bash
cd project-front
npm install
npm run dev      # Vite 开发服务器
npm run build    # 生产构建
npm run preview  # 预览生产构建
```

### 管理端前端
```bash
cd project-front-admin
npm install
npm run dev
npm run build
npm run preview
```

### AI Agent（Python）
```bash
cd project-ai-agent
pip install -r requirements.txt
python main.py           # 在 8000 端口启动 uvicorn
# 或直接使用：
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

## 架构

### 后端模块依赖关系

```
blog-application（启动入口，聚合所有模块）
  ├── blog-article      （文章 CRUD、分类、专栏）
  ├── blog-comment      （评论）
  ├── blog-interaction   （点赞、收藏、关注、私信、通知、浏览历史）
  ├── blog-question      （问答）
  ├── blog-ai            （AI 聊天、摘要、审核、写作 Agent）
  ├── blog-search        （ES 搜索与同步）
  ├── blog-security      （JWT 认证、用户管理）
  ├── blog-job           （XXL-Job 定时任务）
  ├── blog-push          （WebSocket + SSE 推送通道）
  ├── blog-notification  （通知持久化）
  ├── blog-message       （消息）
  ├── blog-point         （积分系统）
  ├── blog-coupon        （优惠券）
  └── blog-vip           （VIP/会员）
       ↓
blog-common（工具库、共享 DTO/VO、MQ 配置、异常处理）
blog-api（跨模块 Feign 接口契约 —— 纯接口，不含实现）
```

### 跨模块通信模式

模块间通过 `blog-api` 中定义的接口通信。提供方实现接口，消费方通过 Spring DI 注入。**禁止**跨业务模块直接 import 类。

```java
// blog-api：定义接口
public interface ArticleInfoAPI { ... }

// blog-article：实现接口
@Service
public class ArticleServiceImpl implements IArticleService, ArticleInfoAPI { ... }

// blog-comment：注入并使用
@Service
@RequiredArgsConstructor
public class CommentServiceImpl {
    private final ArticleInfoAPI articleInfoAPI;
}
```

### Java ↔ Python 通信

- Java 调用 Python：通过 Feign（Python 注册到 Nacos，服务名 `python-ai`）
- Python 调用 Java：通过 HTTP 调用内部接口（`/internal/` 路径，由 ApiKeyAuthFilter 验证）
- 两者都注册到 Nacos 实现服务发现

### 前端请求层

两套前端均使用**原生 `fetch`**（非 Axios），封装在 `utils/request.js` 中。关键细节：
- Token 通过 `token` 请求头传递（非 `Authorization: Bearer`）
- 使用 `json-bigint` 库处理后端返回的 Long 类型 ID（雪花算法 ID 超出 JS `Number.MAX_SAFE_INTEGER`）
- 支持 Refresh Token 自动刷新机制

### 实时通信

- **WebSocket**（`/ws`）：私信、在线状态、未读计数 —— 双向通信，每用户单连接
- **SSE**（`/interaction/sse/connect`）：业务通知（点赞、评论、关注、审核） —— 单向推送，支持多标签页

### 消息队列（RabbitMQ）

所有 MQ 配置集中在 `blog-common/.../config/mqConfig/` 下，按领域划分。每个队列都配置了死信交换机（DLX）。Handler 使用手动 ACK（`channel.basicAck()`）。

## 关键规范

### 详细规则（代码修改时请阅读）

- `.claude/rules/coding-style.md` — Java/Python/Vue 命名、注入模式、MyBatis 规范
- `.claude/rules/api-design.md` — RESTful API 设计、响应格式（`JsonData<T>`）、分页、跨模块 API
- `.claude/rules/architecture.md` — 模块结构、数据库规范、Entity 模板
- `.claude/rules/mq-patterns.md` — RabbitMQ 配置、Handler 模板、死信队列监控
- `.claude/rules/realtime.md` — WebSocket/SSE 生命周期、通知推送模式

### 核心规则

- **Long ID 序列化**：所有 VO/DTO 的 Long 类型字段必须加 `@JsonSerialize(using = ToStringSerializer.class)`，防止前端 JS 精度丢失
- **依赖注入**：统一使用 `@RequiredArgsConstructor` + `private final` 字段 —— 禁止 `@Autowired`
- **事务管理**：所有写操作必须加 `@Transactional(rollbackFor = Exception.class)`
- **跨模块调用**：必须通过 `blog-api` 接口 —— 禁止直接 import 其他模块的类
- **复杂 SQL**：必须写在 XML Mapper 文件中，不得使用注解
- **Python 配置**：必须通过 `config/settings.py` 的 `Settings` 类读取 —— 禁止直接 `os.getenv`
- **Python 日志**：统一使用 `loguru` —— 禁止标准库 `logging`
- **Python 单例**：使用模块级变量 + 工厂函数模式（非类单例）

### 任务行为

- 修改代码后**不要**主动运行构建/测试/编译命令 —— 由用户自行验证
- 完成任务后必须总结：修改了什么、涉及哪些文件、带来什么改进
- 需求不明确时，向用户提问并提供具体选项，而非猜测
