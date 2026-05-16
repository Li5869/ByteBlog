# 实时通信规范 (WebSocket + SSE)

## 双通道架构

项目采用 WebSocket + SSE 双通道设计，职责分离：

| 通道 | 用途 | 连接方式 | 特点 |
|------|------|---------|------|
| WebSocket | 私信、在线状态、未读计数 | `ws://host/ws?token=xxx` | 双向通信，单连接/用户 |
| SSE | 业务通知（点赞/评论/关注/审核） | `GET /interaction/sse/connect?token=xxx` | 单向推送，多连接/用户（多标签页） |

两者共享 `OnlineStateService`（Redis Set）进行在线状态管理。

---

## WebSocket 通道

### 服务端组件

| 文件 | 职责 |
|------|------|
| `blog-push/.../websocket/WebSocketConfig.java` | 注册 `/ws` 端点，配置 `setAllowedOrigins("*")` |
| `blog-push/.../websocket/WebSocketInterceptor.java` | 握手拦截，从 query param 提取 token 验证 |
| `blog-push/.../websocket/WebSocketHandler.java` | 连接管理、消息分发、心跳、粉丝广播 |
| `blog-push/.../websocket/WebSocketMessage.java` | 消息实体 + 工厂方法 |
| `blog-push/.../service/OnlineStateService.java` | Redis 在线状态管理 |

### 消息类型

| 类型 | 方向 | 用途 |
|------|------|------|
| `welcome` | Server → Client | 连接成功，返回 userId + serverTime |
| `user_online` | Server → Client | 用户上线/下线广播（data.online = true/false） |
| `online_status` | Server → Client | 批量在线状态查询响应 |
| `query_online` | Client → Server | 批量查询在线状态（传入 userIds） |
| `private_message` | Server → Client | 私信推送 |
| `unread_update` | Server → Client | 未读计数增量推送 |
| `error` | Server → Client | 错误消息 |

### 连接生命周期

1. **握手**：`WebSocketInterceptor.beforeHandshake()` 从 query param 提取 token，调用 `TokenValidationApi` 验证
2. **建立**：`afterConnectionEstablished()` — 单连接强制（踢掉旧连接）→ 写入 `USER_SESSIONS`/`SESSION_USERS` 双 Map → `OnlineStateService.userOnline()` → 粉丝广播
3. **心跳**：客户端每 30s 发 `"ping"`，服务端回 `"pong"` 并续期 Redis TTL
4. **断开**：`afterConnectionClosed()` — 移除 Map → `userOffline()` → 粉丝广播

### 单连接强制机制

```java
// 先移除旧 session 的映射（避免 close 回调触发 offline 逻辑）
SESSION_USERS.remove(oldSession.getId());
USER_SESSIONS.remove(userId);
// 再关闭旧 session
oldSession.close(CloseStatus.NOT_ACCEPTABLE);
```

### 粉丝广播

```java
// 获取粉丝列表 → 只推给在线粉丝
List<Long> followerIds = followerApi.getFollowerIds(userId);
for (Long followerId : followerIds) {
    WebSocketSession session = USER_SESSIONS.get(followerId);
    if (session != null && session.isOpen()) {
        session.sendMessage(new TextMessage(json));
    }
}
```

---

## SSE 通道

### 服务端组件

| 文件 | 职责 |
|------|------|
| `blog-interaction/.../controller/SseController.java` | SSE 端点 `GET /interaction/sse/connect` |
| `blog-push/.../sse/SseEmitterManager.java` | 多标签页连接管理，消息推送 |

### 连接建立

```java
@GetMapping(value = "/interaction/sse/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter connect() {
    Long userId = UserContextHolder.getUserId();
    SseEmitter emitter = sseEmitterManager.createEmitter(userId);
    // 发送连接成功事件
    emitter.send(SseEmitter.event().name("connect").data("SSE 连接成功"));
    return emitter;
}
```

### SseEmitterManager 特点

- `ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>` — 支持同一用户多标签页
- 超时设置 `0L`（永不超时）
- `onCompletion`/`onTimeout`/`onError` 回调自动清理死连接
- `sendToUser(userId, message)` 遍历所有连接发送，死连接延迟清理

### 前端 SSE 客户端

```js
// sse.js — SseManager 类
connect() {
    const eventSource = new EventSource(`${baseUrl}/interaction/sse/connect?token=${token}`);
    eventSource.addEventListener('connect', () => { this.reconnectAttempts = 0; });
    eventSource.addEventListener('notification', (e) => {
        const data = JSONbig.parse(e.data);
        this.emit('notification', data);
    });
}
// 重连：固定 3s 延迟，最多 5 次
```

---

## 通知推送模式

### 三种推送模式

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| **MQ → DB** | 只存 DB，不实时推送 | 点赞、收藏 |
| **MQ → DB + SSE** | 存 DB + 实时 SSE 推送 | 关注 |
| **Async → DB + SSE** | CompletableFuture 异步存 DB + SSE 推送 | 评论、问答、审核 |
| **Direct → DB + WS** | 直接调用存 DB + WebSocket 推送 | 私信、未读计数 |

### 通知发送模板（SSE 推送）

```java
// 1. 保存通知到 DB
Long notificationId = notificationApi.saveNotification(messageDTO);

// 2. 保存成功后实时推送
if (notificationId != null) {
    messageDTO.setId(notificationId);
    sseEmitterManager.sendToUser(receiverId, messageDTO);
}
```

### 通知发送模板（仅存 DB）

```java
CompletableFuture.runAsync(() -> {
    notificationApi.saveNotification(messageDTO);
}, executor).exceptionally(e -> {
    log.error("通知保存失败: {}", e.getMessage());
    return null;
});
```

---

## 在线状态管理 (OnlineStateService)

### Redis 数据结构

| Key | 类型 | TTL | 用途 |
|-----|------|-----|------|
| `online:users` | Set | 无 | 在线用户 ID 集合 |
| `user:online:{userId}` | String | 300s | 用户在线详情（loginTime, device） |

### 核心方法

- `userOnline(userId, loginTime)` — SADD + SET 详情
- `userOffline(userId)` — SREM + DEL 详情
- `heartbeat(userId)` — 续期 TTL，处理 Set 被驱逐的边界情况
- `isOnline(userId)` — SISMEMBER 检查
- `batchGetOnlineStatus(userIds)` — Pipeline 批量查询

### 共享使用

- **WebSocket Handler**：连接/断开时调用 `userOnline()`/`userOffline()`
- **NotificationMqHandler**：推送通知前调用 `isOnline()` 检查
- **前端**：通过 `query_online` 消息批量查询

---

## 前端集成

### 连接初始化（App.vue）

```js
onMounted(() => {
    if (isLoggedIn()) {
        wsManager.connect();
        sseManager.connect();
    }
    // 跨 Tab 登录/登出同步
    window.addEventListener('storage', handleStorageChange);
});
```

### 登录后连接（stores/user.js）

```js
// 登录成功后
wsManager.connect();
sseManager.connect();

// 登出后
wsManager.disconnect();
sseManager.disconnect();
```

### 通知展示组件

| 组件 | 通道 | 触发事件 | 展示形式 |
|------|------|---------|---------|
| `NotificationToast.vue` | SSE | `sseManager.on('notification')` | Toast 弹窗，5s 自动关闭 |
| `MessageNotification.vue` | WebSocket | `wsManager.on('private_message')` | Toast 弹窗，智能抑制（当前聊天用户不弹） |
