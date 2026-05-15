# ByteBlog 项目规则

## 项目架构概览

ByteBlog 是一个前后端分离的博客系统，包含 3 个子项目：

| 子项目 | 路径 | 技术栈 | 说明 |
|--------|------|--------|------|
| 后端 | `project-backen/` | Java 21 + Spring Boot 3 + MyBatis-Plus + PostgreSQL + Redis + RabbitMQ + Elasticsearch | 12 个 Maven 模块 |
| 用户端前端 | `project-front/` | Vue 3 + Tailwind CSS + Naive UI | 原生 fetch，无 Axios |
| 管理端前端 | `project-front-admin/` | Vue 3 + Element Plus + Pinia | 独立的管理后台 |
| AI Agent | `project-ai-agent/` | Python + FastAPI + LangGraph + LangChain | AI 聊天/写作/知识库 |

### 后端模块依赖关系

```
blog-application (启动入口)
  ├── blog-article      (文章 CRUD)
  ├── blog-comment      (评论)
  ├── blog-interaction   (点赞/收藏/关注/私信/通知/浏览历史)
  ├── blog-question      (问答)
  ├── blog-ai            (AI 聊天/摘要/审核/写作)
  ├── blog-search        (ES 搜索/同步)
  ├── blog-security      (认证/用户)
  ├── blog-job           (定时任务)
  └── blog-push          (WebSocket + SSE 推送)
       ↓
blog-common (工具库/配置/共享 DTO/VO)
blog-api (跨模块 Feign 接口契约)
```

### 核心业务流程

详细的数据流图和业务流程图见 `docs/diagrams/ByteBlog核心业务流程图.md`。

---

## 1. 任务完成后的行为
- 需求开发或代码修改完成后，**不要**主动运行终端命令进行编译、构建或测试。
- 所有编译、测试、验证操作由用户自行执行并检查。

## 2. 任务总结要求
- 每次完成一个任务后，必须在回复中进行总结，至少包含以下内容：
  - 本次修改涉及的需求或问题。
  - 修改了哪些文件、哪些具体代码（如有必要可附上关键代码片段）。
  - 本次修改带来的优点或改进，例如性能提升、可读性增强、逻辑简化等。

## 3. 不确定性处理
- 遇到需求不明确、技术方案存在多种可行选择，或对项目上下文有不确定的地方时，**必须**主动调用工具或通过对话向用户提问，而非基于猜测直接编码。
- 提问应尽量具体，提供1-2个可选项或明确表达不确定的点。

## 4. 代码规范
- 所有生成的代码必须遵循项目已有的规范或语言通用规范。
- 关键逻辑、复杂算法、非常规写法必须附有注释，注释应说明"为什么这么做"而非仅仅复述代码。
- 确保用户能够在不依赖外部文档的情况下，仅通过注释即可理解代码意图。
