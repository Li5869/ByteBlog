# 代码风格规则

## 通用原则

- 所有代码必须遵循项目已有的规范或语言通用规范
- 关键逻辑、复杂算法、非常规写法必须附有注释，注释说明"为什么这么做"而非仅仅复述代码
- 确保不依赖外部文档，仅通过注释即可理解代码意图
- 代码提交前检查无用的 mock 数据、调试日志和注释掉的代码，保持代码整洁

---

## Java 编码规范

### 基础规范
- 遵循阿里巴巴 Java 开发规范
- 采用 RESTful API 设计风格
- 作者标注：`@author LSH`

### 命名约定
| 元素 | 规范 | 示例 |
|------|------|------|
| 包名 | `com.personblog.{模块名}` | `com.personblog.article` |
| 类名 | 大驼峰命名 | `ArticleController`, `ArticleService` |
| 方法名 | 小驼峰命名 | `getArticleById`, `createArticle` |
| 常量 | 全大写下划线分隔 | `MAX_PAGE_SIZE` |

### Long 类型 ID 序列化（重要）
- 所有 VO/DTO 类中的 Long 类型 ID 字段必须添加 `@JsonSerialize(using = ToStringSerializer.class)` 注解
- 目的：防止前端 JavaScript 精度丢失（雪花算法 ID 超过 JS 最大安全整数 2^53-1）
- 示例：
  ```java
  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  ```

### 三层架构规范
- 必须严格遵循 Controller → Service → Mapper 三层架构
- Controller 层：处理 HTTP 请求，参数校验，调用 Service
- Service 层：业务逻辑处理，事务管理
- Mapper 层：数据库访问

### 依赖注入
- **全部使用构造器注入**，结合 Lombok `@RequiredArgsConstructor`
- 所有 `private final` 字段自动被注入禁止使用 `@Autowired`
- 仅当需要指定 Bean 名称时使用 `@Resource(name = "...")`

### 类继承与实现模式
- Service 实现类：继承 `ServiceImpl<{Entity}Mapper, {Entity}>`，同时实现模块内接口和跨模块 API 接口
  ```java
  @Service
  @RequiredArgsConstructor
  @Slf4j
  public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
          implements IQuestionService, QuestionApi {
  ```
- Controller：纯 POJO，不继承任何基类
  ```java
  @Slf4j
  @Tag(name = "认证管理", description = "用户登录、注册、登出等接口")
  @RestController
  @RequestMapping("/auth")
  @RequiredArgsConstructor
  public class AuthController {
  ```

### MyBatis SQL 规范
- 简单的单表 CRUD 使用 MyBatis Plus 内置方法或 `@Select` 等注解
- **复杂 SQL 语句必须写入 XML 文件**，包括但不限于：
  - 多表关联查询（JOIN）
  - 包含子查询的语句
  - 动态 SQL（使用 `<if>`、`<foreach>` 等标签）
  - 批量操作语句
- XML 文件位置：`src/main/resources/mapper/{EntityName}Mapper.xml`
- MyBatis Plus 操作模式：
  - 简单查询：`lambdaQuery().eq(Entity::getField, value).one()`
  - 简单更新：`lambdaUpdate().eq(Entity::getId, id).set(Entity::getField, value).update()`
  - SQL 表达式更新：`lambdaUpdate().setSql("field=field+" + delta)`
  - 动态条件：`.and(condition, wrapper -> wrapper.eq(...))`

### 分页规范
- 统一使用 MyBatis Plus `Page` 对象
- 默认值：current=1, size=10
- 上限保护：`Math.min(size, MAX_SIZE)`
  ```java
  int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
  int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);
  Page<Entity> page = new Page<>(current, size);
  page(page, queryWrapper);
  ```

### GET 请求 DTO 规范
- GET 请求参数超过 3 个时，必须定义 DTO 进行传输
- DTO 放在 blog-common 或对应模块的 dto 包下
- 使用 `@Schema(description = "...")` 注解描述

### 事务管理
- 所有写操作（增、删、改）必须加 `@Transactional(rollbackFor = Exception.class)`

### 参数校验
- Controller 层：使用 `@Valid` 注解
- Service 层：手动校验 DTO 字段，带具体错误信息

### 异常处理
- 业务异常：抛出 `BizException`，携带 `BizCodeEnum` 枚举
- 非关键异常：捕获并用 `log.warn` 记录，不影响主流程
  ```java
  throw new BizException(BizCodeEnum.USER_NOT_EXIST);
  throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题ID无效");
  ```

### 注释风格
- 类注释：Javadoc 风格，包含 `@author` 和 `@since`
- 方法注释：简洁中文 Javadoc，说明业务逻辑
- 行内注释：简洁中文，在关键逻辑前标注，不在行尾
- 复杂方法内用 `// 1. ...`、`// 2. ...` 编号标注流程步骤
- 所有注释必须说明"为什么这么做"而非复述代码

### 响应格式
- 统一使用 `JsonData<T>`：
  ```java
  return JsonData.buildSuccess(userInfoVO);              // 成功，带数据
  return JsonData.buildSuccess();                        // 成功，无数据
  return JsonData.buildResult(BizCodeEnum.LOGIN_ERROR);  // 业务错误
  ```

### 缓存策略
- 本地缓存：使用 Caffeine，在 `@PostConstruct` 中初始化
- 多级缓存：通过 `MultiLevelCacheUtil` 实现 Redis + Caffeine 双级缓存
- 写操作后主动失效缓存：`cacheUtil.evict(KEY)`

### 异步处理
- 方式一：`CompletableFuture.runAsync(() -> ..., executor).exceptionally(e -> {...})`
- 方式二：`@Async("ExecutorName")` 注解 + 自定义线程池
- 所有异步操作必须带 `.exceptionally()` 兜底

### 工具类使用
- 优先使用 Hutool 工具库：`cn.hutool.core` 下的工具类
- 常量使用 `import static` 导入

### 日志规范
- 统一使用 Lombok `@Slf4j`
- 日志包含上下文信息：`log.info("用户登录成功: userId={}", userId)`
- 日志级别：info（正常流程）、warn（可恢复问题）、error（失败）

---

## Python 编码规范

### 基础规范
- 遵循 PEP 8 编码规范
- 使用 Black 格式化代码
- 类型注解：所有函数参数和返回值必须标注类型

### 命名约定
| 元素 | 规范 | 示例 |
|------|------|------|
| 包名/目录 | 小写下划线 | `agents`, `services`, `tools` |
| 类名 | 大驼峰 | `RAGAgent`, `WritingAgent` |
| 函数名 | 小写下划线 | `generate_summary`, `retrieve_documents` |
| 常量 | 全大写下划线 | `MAX_TOKENS`, `DEFAULT_TEMPERATURE` |
| 私有方法 | 下划线前缀 | `_build_graph()`, `_init_once()` |
| 私有模块变量 | 下划线前缀 | `_rag_agent`, `_article_es_service` |

### 导入规范
- 标准库优先，然后第三方库，最后本项目模块，各段之间有空行分隔
- 内部模块使用相对/绝对导入

### 日志规范
- 统一使用 `loguru`（禁止使用标准库 logging）
- 格式：`f"[ModuleName] 描述: {变量}"`
- 日志级别：`logger.info`（正常流程）、`logger.warning`（可恢复问题）、`logger.error`（失败）

### 注释风格
- 模块 docstring：文件开头，说明文件用途
- 函数注释：Google 风格 docstring，包含 Args 和 Returns
- 行内注释：`#` 开头，注释内容与 `#` 之间有一个空格，放在代码上方
- 可用类比方式帮助 Java 开发者理解 Python 概念：`# 类比 Java 的 @Component + @Lazy`

### 单例模式
- 统一使用"模块级变量 + 工厂函数"方式
  ```python
  _rag_agent: Optional[RAGAgent] = None
  async def get_rag_agent() -> RAGAgent:
      global _rag_agent
      if _rag_agent is None:
          _rag_agent = RAGAgent()
          await _rag_agent.initialize()
      return _rag_agent
  ```
- 配置管理使用 `@lru_cache` 装饰器

### LangGraph 工作流规范
- 使用 `TypedDict` 定义状态
- 每个节点是纯函数，接收状态返回状态更新
- 使用 `StateGraph` 构建工作流
- 工作流构建步骤：创建 `StateGraph` → `add_node` → `set_entry_point` → `add_edge` / `add_conditional_edges` → `compile`
- Agent 类提供 `run()`（同步）和 `arun()`（异步）两种调用方式

### FastAPI 接口规范
- 路由前缀：`/api/v1/{模块}`
- 使用 Pydantic 模型进行请求验证
- 统一响应格式与 Java 后端保持一致
- 支持 SSE 流式响应

### 配置管理
- 统一使用 `config/settings.py` 的 `Settings` 类（Pydantic v2 BaseSettings）
- 禁止直接 `os.getenv`
- 敏感信息通过环境变量配置

### 代码分区
- 使用 `# ==================== 分隔线 ====================` 对文件进行视觉分区
- 类比 Java 中使用 `// =====` 或 `/** */` 分段

---

## 前端编码规范

### 基础规范
- 采用 Vue 3 组合式 API 开发规范（`<script setup>`）
- 使用 TypeScript 类型约束
- 不强制使用 TypeScript，可使用 `.vue` + `.js` 组合

### 命名约定
| 元素 | 规范 | 示例 |
|------|------|------|
| 组件名 | 大驼峰 | `ArticleCard.vue` |
| 变量/方法 | 小驼峰 | `const userInfo = ref({})` |
| CSS 类名 | Tailwind CSS 或小写连字符 | `class="article-card"` |

### 组件定义模式
- 全部使用 `<script setup>` 组合式 API
- Props 使用 `defineProps` 宏，设置类型、默认值和验证器
  ```js
  defineProps({
    userId: { type: [Number, String], required: true },
    size: { type: String, default: 'md' }
  })
  ```

### 状态管理
- 用户端：使用 `reactive` + composable 模式（非强制使用 Pinia）
- 管理端：状态集中在 request.js 中管理

### 请求封装
- 基于原生 `fetch` 封装（不使用 Axios）
- Token 通过 `token` 请求头传递（非 `Authorization: Bearer`）
- 支持 Refresh Token 自动刷新机制

### Long 数据处理（重要）
- JavaScript Number 最大安全整数为 2^53 - 1 (9007199254740991)
- 后端雪花算法生成的 Long 类型 ID 会超过此范围，导致精度丢失
- 用户端必须使用 `json-bigint` 库处理包含 Long 类型 ID 的 JSON 数据
- 配置示例：`JSONbig({ storeAsString: true })` 将大整数转为字符串
- 在组件中将 userId 显式转为字符串：`String(props.userId)`

### API 定义模式
- 以对象形式集中管理，每个 API 方法对应一个后端接口
  ```js
  export const articleApi = {
    getArticlePage: (data) => post('/article/articles', data),
    getArticleDetail: (id) => get(`/article/articles/${id}`),
    createArticle: (data) => post('/article/articles/publish', data),
  }
  ```

### 样式规范
- 大量使用 Tailwind CSS 原子化类名
- 全面支持暗黑模式（`dark:` 前缀类名）
- 动画效果采用 `transition-all duration-300` + `hover:transform`

### Composables 组合式函数
- 遵循 `useXxx` 命名规范
- 返回响应式状态和方法
- 在 `onMounted` 中注册事件，`onUnmounted` 中解注册
