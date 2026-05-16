package com.personblog.push.websocket;

import cn.hutool.json.JSONUtil;
import com.personblog.common.api.FollowerApi;
import com.personblog.push.onlineMessage.PushMessage;
import com.personblog.push.service.OnlineStateService;
import com.personblog.push.service.PushChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 处理器
 * 处理用户连接、断开、心跳、消息
 *
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    // 处理用户连接建立
    private final OnlineStateService onlineStatusApi;
    // 粉丝查询接口，由 blog-interaction 模块实现
    private final FollowerApi followerApi;
    // 跨节点消息推送服务
    private final PushChannelService pushChannelService;
    // 存储用户会话
    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();
    // 存储会话与用户ID的映射关系
    private static final Map<String, Long> SESSION_USERS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        Long loginTime = (Long) session.getAttributes().get("loginTime");

        if (userId == null) {
            log.warn("[WebSocket] 连接建立失败: 缺少 userId");
            session.close();
            return;
        }

        // 如果该用户已有旧连接，先踢掉旧连接（不触发下线逻辑）
        WebSocketSession oldSession = USER_SESSIONS.put(userId, session);
        if (oldSession != null && oldSession.isOpen()) {
            // 先从 SESSION_USERS 移除旧映射，防止旧连接关闭时走 afterConnectionClosed 逻辑
            SESSION_USERS.remove(oldSession.getId());
            try {
                oldSession.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (Exception e) {
                log.warn("[WebSocket] 关闭旧连接失败: {}", e.getMessage());
            }
        }

        // 存储会话与用户ID的映射关系
        SESSION_USERS.put(session.getId(), userId);
        // 更新用户在线状态
        onlineStatusApi.userOnline(userId, loginTime);

        // 发送欢迎消息
        sendMessage(session, WebSocketMessage.welcome(userId));
        // 通知该用户的粉丝：该用户上线了
        broadcastToFollowers(userId, WebSocketMessage.userOnline(userId));
    }
    // 处理用户消息
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 从会话中获取用户ID
        Long userId = SESSION_USERS.get(session.getId());
        // 检查用户是否存在
        if (userId == null) {
            return;
        }
        // 解析消息
        String payload = message.getPayload();
        // 处理心跳消息
        if ("ping".equals(payload)) {
            onlineStatusApi.heartbeat(userId);
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        try {
            // 解析消息
            WebSocketMessage wsMessage = JSONUtil.toBean(payload, WebSocketMessage.class);
            // 处理消息类型
            handleMessageByType(session, userId, wsMessage);
        } catch (Exception e) {
            sendMessage(session, WebSocketMessage.error("消息格式错误"));
        }
    }
    // 处理用户连接关闭
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 从会话中获取用户ID
        Long userId = SESSION_USERS.remove(session.getId());
        // 检查用户是否存在
        if (userId != null) {
            USER_SESSIONS.remove(userId);
            onlineStatusApi.userOffline(userId);
            // 通知该用户的粉丝：该用户下线了
            broadcastToFollowers(userId, WebSocketMessage.userOffline(userId));
            log.info("[WebSocket] 连接关闭: userId={}, status={}", userId, status);
        }
    }
    // 处理用户传输错误
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[WebSocket] 传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());

        if (session.isOpen()) {
            session.close();
        }
    }
    // 发送消息给指定用户（通过 Redis Pub/Sub 分发到所有节点，包括自身）
    public void sendToUser(Long userId, WebSocketMessage message) {
        String json = JSONUtil.toJsonStr(message);
        pushChannelService.publish(new PushMessage(
                PushChannelService.CHANNEL_WS, userId, json, null));
    }

    /**
     * 仅本地投递（供 PushChannelService 跨节点回调使用）
     */
    public void sendToLocalUser(Long userId, String json) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("[WebSocket] 本地投递失败: userId={}", userId, e);
            }
        }
    }
    // 通知用户的粉丝（通过 Redis Pub/Sub 广播到所有节点，同时走 WS 和 SSE 两个通道）
    private void broadcastToFollowers(Long userId, WebSocketMessage message) {
        List<Long> followerIds = followerApi.getFollowerIds(userId);
        if (followerIds.isEmpty()) {
            return;
        }
        String json = JSONUtil.toJsonStr(message);
        // 1. WebSocket 广播
        pushChannelService.publish(new PushMessage(
                PushChannelService.CHANNEL_WS_BROADCAST, userId, json, followerIds));
        // 2. SSE 广播（在线状态事件）
        boolean isOnline = WebSocketMessage.TYPE_USER_ONLINE.equals(message.getType())
                && message.getData() instanceof Map<?, ?> data
                && Boolean.TRUE.equals(data.get("online"));
        String ssePayload = JSONUtil.toJsonStr(Map.of(
                "userId", String.valueOf(userId),
                "online", isOnline));
        pushChannelService.publish(new PushMessage(
                PushChannelService.CHANNEL_SSE_BROADCAST, userId, ssePayload, followerIds));
    }
    // 发送消息给指定会话
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        if (session == null || !session.isOpen()) {
            log.warn("[WebSocket] 会话未打开，跳过发送");
            return;
        }
        try {
            String json = JSONUtil.toJsonStr(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("[WebSocket] 发送消息失败", e);
        }
    }
    // 查询在线用户状态
    private void handleMessageByType(WebSocketSession session, Long userId, WebSocketMessage msg) {
        String type = msg.getType();
        // 处理查询在线用户状态消息
        if (WebSocketMessage.TYPE_QUERY_ONLINE.equals(type)) {
            Object data = msg.getData();
            if (data instanceof List<?> rawList) {
                // Jackson 反序列化 JSON 数字为 Integer，需手动转为 Long
                List<Long> userIds = rawList.stream()
                        .map(item -> {
                            if (item instanceof Number num) {
                                return num.longValue();
                            }
                            return Long.valueOf(item.toString());
                        })
                        .toList();
                Map<Long, Boolean> status = onlineStatusApi.batchGetOnlineStatus(userIds);
                sendMessage(session, WebSocketMessage.onlineStatus(status));
            }
        } else {
            log.warn("[WebSocket] 未知消息类型: {}", type);
        }
    }
}
