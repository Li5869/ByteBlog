# AI 写作任务创建重复与 401 认证失败问题修复

## 问题描述

用户登录后在 AI 写作页面点击"开始写作"，现象如下：

1. 前端显示"开始创作"界面（第一次请求成功）
2. 过一会儿弹回提示"未登录，请先登录"
3. 后端日志显示创建了**两个**写作任务
4. 第二个任务创建请求始终返回 401，即使前端刷新 Token 后重试也仍然失败

## 排查历程

### 第一阶段：前端重试逻辑

**怀疑**：前端 `createWritingTaskDirect` 函数手动处理 401 刷新，但逻辑不够健壮，导致重复创建任务。

**分析**：日志显示第一次请求成功创建任务，约 1.2 秒后第二个请求认证失败。前端 Token 刷新成功后第三次请求又创建了新任务。

**修复**：
- 化简 `createWritingTaskDirect` 为直接调用 `aiApi.createWritingTask`，由 `request.js` 统一处理 401
- 修改按钮为 `disabled` 状态，防止重复点击
- 添加 stream 请求失败时的自动重连（最多 3 次）

**结果**：❌ 问题依旧

### 第二阶段：POST 请求重试策略

**怀疑**：`request.js` 的 `request()` 函数在 401 时无条件重试，对于 POST 等非幂等请求可能导致重复操作。

**分析**：但 401 表示请求根本没有被后端业务逻辑处理（认证失败在过滤器层就拒绝了），理论上重试是安全的。然而前端日志显示刷新 Token 后重试仍然返回 401。

**修复**：
- 保持所有请求（包括 POST）在 401 时自动刷新 Token 并重试
- 后端 `AuthController.refresh()` 中调用 `kickOutUser()` 删除旧 Token

**结果**：❌ 问题依旧

### 第三阶段：后端 Redis 登录信息丢失

**怀疑**：Token 刷新时 `kickOutUser()` 删除了旧 Token 的 Redis 登录信息，导致正在进行的并发请求（使用旧 Token）无法通过认证。

**分析**：后端日志确认了这一点——刷新 Token 的请求（handler-10）调用了 `kickOutUser`，而另一线程（handler-11）使用被删除的旧 Token 请求，JWT 签名验证通过但 Redis 中无数据。

**修复**：在 `JwtAuthenticationTokenFilter` 中，当 JWT 有效但 Redis 无登录信息时，根据 JWT 中的用户 ID 重建最小化认证。

**结果**：❌ 问题依旧

### 第四阶段（最终定位）：Spring MVC 异步调度安全上下文丢失

**怀疑**：控制器 `WritingAgentController.createAndStart()` 返回 `Mono<JsonData<WritingTaskVO>>`，属于异步请求。

**分析**：添加过滤器级别的请求追踪日志后，发现认证失败的请求**没有**经过 JWT 过滤器的日志输出。

**根本原因**：Spring MVC 对异步请求的处理流程：
1. 第一次请求（`DispatcherType.REQUEST`）：JWT 过滤器正常执行，设置认证
2. 控制器返回 `Mono`，请求线程归还 Tomcat 线程池
3. `Mono` 结果就绪（Python 服务返回），Tomcat 从线程池获取新线程
4. 新线程触发异步调度（`DispatcherType.ASYNC`）
5. **`OncePerRequestFilter` 默认跳过异步调度**，新线程中没有 `SecurityContext`
6. 安全过滤器链检测到无认证 → 触发 `AuthenticationEntryPoint` → 返回 401

## 修复方案

### 核心修复

在 `JwtAuthenticationTokenFilter` 中重写 `shouldNotFilterAsyncDispatch()` 方法：

```java
@Override
protected boolean shouldNotFilterAsyncDispatch() {
    return false;
}
```

确保异步调度时也执行过滤器，从请求头重新提取 Token 进行认证。

### 辅助修复

1. **JWT 过滤器：Redis 无数据时重建基础认证**（`else` 分支）
   当 JWT 签名验证通过但 Redis 中无登录信息时（Token 刷新后旧 Token 被踢出），根据 JWT 中的用户 ID 构建最小化认证，确保正在进行的请求不被中断。

2. **后端幂等性检查**（`WritingTaskServiceImpl.createTask()`）
   10 秒内相同用户的 `planning` 状态任务直接返回已有任务，防止任何情况下的重复创建。

3. **前端统一刷新机制**（`request.js`）
   所有请求（包括 POST）在 401 时自动使用统一逻辑刷新 Token 并重试，避免冗余的手动刷新代码导致竞态条件。

## 修改文件清单

| 文件 | 修改内容 | 关键代码行 |
|------|----------|-----------|
| `blog-security/.../JwtAuthenticationTokenFilter.java` | 允许异步调度时执行过滤器 + Redis 无数据时重建认证 | `shouldNotFilterAsyncDispatch()` 第138行 |
| `blog-ai/.../WritingTaskServiceImpl.java` | 添加幂等性检查 | `createTask()` 第43-56行 |
| `blog-ai/.../WritingAgentController.java` | 幂等命中时跳过 Python 服务调用 | `createAndStart()` 第58-66行 |
| `blog-security/.../AuthController.java` | 刷新时删除旧 Token | `refresh()` 第299行 |
| `project-front/.../request.js` | 统一 401 刷新机制 | `request()` 第151-252行 |
| `project-front/.../AIWriting.vue` | 简化创建任务函数 | `createWritingTaskDirect()` 第284-287行 |

## 为什么引入双 Token + Nacos 后才出现？

| 因素 | 引入前 | 引入后 |
|------|--------|--------|
| Token 刷新频率 | 单 Token + 7 天过期 | 双 Token + 30 分钟过期 |
| 旧 Token 清理 | 无 `kickOutUser` | 刷新时自动踢出 |
| 线程池压力 | 低（无 Nacos） | 高（Nacos 心跳 + 订阅 + 负载均衡） |
| 异步调度概率 | 低（线程充足） | 高（线程被 Nacos 占用） |
| 问题暴露 | 很难触发 | 频繁触发 |

核心是一个**隐藏的异步安全问题**（`OncePerRequestFilter` 不处理异步调度），在双 Token + Nacos 引入后，由于 Token 刷新频率增加和线程池压力增大，问题被频繁触发。

## 最终防护架构

```
┌─────────────────────────────────────────────────┐
│              前端 request.js                      │
│  401 时自动刷新 Token 并重试请求                   │
└─────────────────────┬───────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────┐
│       后端 JwtAuthenticationTokenFilter           │
│  ① 异步调度时重新认证（shouldNotFilterAsyncDispatch）│
│  ② Redis 无数据时根据 JWT 重建基础认证             │
└─────────────────────┬───────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────┐
│        后端 WritingTaskServiceImpl                │
│  幂等性检查：10秒内相同用户的任务返回已有任务        │
└─────────────────────────────────────────────────┘
```
