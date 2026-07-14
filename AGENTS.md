# AGENTS.md

ByteBlog：AI 增强全栈技术博客平台，含 4 个子项目。  
完整架构与规范参见 `.opencode/rules/` —— 请先阅读那些文件。

## 子项目与命令

| 项目 | 路径 | 技术栈 | 端口 |
|---|---|---|---|
| 后端 | `project-backend/` | Java 21, Spring Boot 4, Maven | 8080 |
| 用户端前端 | `project-front/` | Vue 3, NaiveUI, Tailwind | 3000 |
| 管理端前端 | `project-front-admin/` | Vue 3, Element Plus, Pinia | 5174 |
| AI Agent | `project-ai-agent/` | Python 3.12+, FastAPI, LangGraph | 8000 |

```bash
# 后端
cd project-backend
mvn clean install -DskipTests                 # 构建全部模块（跳过测试加速）
mvn spring-boot:run -pl blog-application      # 启动应用
mvn test -pl blog-article                     # 运行单个模块测试

# 用户端前端
cd project-front && npm run dev

# 管理端前端
cd project-front-admin && npm run dev

# AI Agent
cd project-ai-agent && python main.py
```

## 高频规范（极易出错）

### Java
- **依赖注入**：`@RequiredArgsConstructor` + `private final` —— 绝不用 `@Autowired`
- **跨模块调用**：必须通过 `blog-api` 模块中的接口。绝不可直接 `import` 其他模块的类。
- **Long ID 序列化**：所有 VO/DTO 的 Long 字段必须加 `@JsonSerialize(using = ToStringSerializer.class)` —— 雪花 ID 超出 JS `Number.MAX_SAFE_INTEGER`。
- **事务**：所有写操作必须加 `@Transactional(rollbackFor = Exception.class)`。
- **复杂 SQL**：必须写在 XML Mapper 文件中。简单 CRUD 才用注解。
- **表命名**：前缀 `tb_`，软删除字段 `is_deleted` 配合 `@TableLogic`，雪花 ID 用 `@TableId(type = IdType.ASSIGN_ID)`。
- **响应格式**：`JsonData<T>`，用 `JsonData.buildSuccess(data)` / `JsonData.buildResult(BizCodeEnum.XXX)`。
- **抛异常**：`throw new BizException(BizCodeEnum.XXX)`。
- **分页**：`Page<Entity>`，current 默认 1，size 默认 10，最大限制 50。
- **GET 参数 > 3 个**：必须用 DTO 接收，不能直接写裸参数。

### MQ（RabbitMQ）
- 所有队列必须配置死信交换机（DLX）。
- Handler 成功时 `channel.basicAck(deliveryTag, false)`，失败时 `channel.basicNack(deliveryTag, false, false)`。
- MQ 配置类放在 `blog-common/.../config/mqConfig/`，用 `public static final` 常量定义名称。

### Python
- **配置**：必须用 `config/settings.py`（`Settings` 类，Pydantic `BaseSettings`）。绝不用 `os.getenv()`。
- **日志**：必须用 `loguru`。绝不用标准库 `logging`。
- **单例**：模块级变量 + 异步工厂函数模式。不用类单例。
- **类型注解**：所有函数参数和返回值必须标注类型。
- **LangGraph 状态**：`TypedDict`，纯函数节点（接收 state，返回部分更新）。

### 前端
- **HTTP 客户端**：原生 `fetch` —— 不用 Axios。
- **Token 头**：`token`（小写自定义头） —— 不用 `Authorization: Bearer`。
- **长整型 ID**：用 `json-bigint`，`storeAsString: true`，处理雪花 ID 精度丢失。
- **用户端 vs 管理端**：localStorage key 不同（`token` / `refresh-token` / `user-info` vs `admin-token` / `admin-refresh-token` / `admin-info`）。API 前缀不同：`/api/` vs `/admin/`。
- **用户端状态**：`reactive` + composable（不用 Pinia）。管理端：Pinia。

### 实时通信
- **WebSocket**（`/ws?token=xxx`）：私信、在线状态 —— 每用户 1 个连接。
- **SSE**（`/interaction/sse/connect?token=xxx`）：通知（点赞、评论、关注） —— 支持多标签页。

## 后端模块架构

17 个 Maven 模块在 `project-backend/` 下。关键约束：
- `blog-common` —— 共享基础设施，被所有模块依赖，不依赖任何模块。
- `blog-api` —— 纯接口契约层（不含实现），被所有业务模块依赖。
- `blog-application` —— 启动模块，聚合所有其他模块。
- 业务模块之间**只能**通过 `blog-api` 接口通信。

## AI 双引擎

- **Spring AI**（`blog-ai` 模块）：同步轻量任务（标题生成、摘要提取、内容审核）。
- **Python LangGraph**（`project-ai-agent/`）：异步复杂工作流（对话采用 Supervisor+Sub-Agent、写作采用固定工作流、深度研究采用 Orchestrator-Worker）。
- Java ↔ Python 通信：Feign（Java 通过 Nacos 服务 `python-ai` 调用 Python）、HTTP `/internal/`（Python 调用 Java，API Key 鉴权）。

## 修改代码后

- 不要自动运行构建/测试 —— 由用户验证。
- 总结：改了什么、涉及哪些文件、带来什么改进。
- 需求不明确时，向用户提问并提供具体选项 —— 不要猜测。
