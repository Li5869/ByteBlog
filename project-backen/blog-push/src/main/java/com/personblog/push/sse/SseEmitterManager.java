package com.personblog.push.sse;

import cn.hutool.json.JSONUtil;
import com.personblog.push.constant.PushConstants;
import com.personblog.push.onlineMessage.PushMessage;
import com.personblog.push.service.PushChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class SseEmitterManager {
    // 存储用户ID与SseEmitter的映射（支持一个用户多个连接）
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // 跨节点消息推送服务
    private final PushChannelService pushChannelService;

    /**
     * 创建 SSE 连接
     * @param userId 用户ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        // 创建 SseEmitter，设置超时时间（0表示永不超时）
        SseEmitter emitter = new SseEmitter(0L);

        // 注册回调（回调中吞掉异常，防止从 Redisson/Servlet 线程逃逸）
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> {
            removeEmitter(userId, emitter);
            log.debug("[SSE] Emitter 异常清理: userId={}, error={}", userId, e.getMessage());
        });

        // 添加到集合
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        return emitter;
    }

    /**
     * 推送消息给指定用户（SSE 事件名默认 notification）
     */
    public void sendToUser(Long userId, Object message) {
        sendToUser(userId, message, "notification");
    }

    /**
     * 推送消息给指定用户（通过 Redis Pub/Sub 分发到所有节点，支持自定义 SSE 事件名）
     */
    public void sendToUser(Long userId, Object message, String eventName) {
        String json = JSONUtil.toJsonStr(message);
        PushMessage msg = new PushMessage(PushConstants.CHANNEL_SSE, userId, json, null);
        msg.setEventName(eventName);
        pushChannelService.publish(msg);
    }

    /**
     * 仅本地投递，事件名默认为 notification（供 PushChannelService 跨节点回调使用）
     */
    public void sendToLocalUser(Long userId, String jsonPayload) {
        sendToLocalUser(userId, jsonPayload, "notification");
    }

    /**
     * 仅本地投递，支持自定义 SSE 事件名
     * @param userId 用户ID
     * @param jsonPayload 已序列化的 JSON 消息
     * @param eventName SSE 事件名（如 "notification"、"user_online"）
     */
    public void sendToLocalUser(Long userId, String jsonPayload, String eventName) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[SSE] 用户无本地连接，跳过: userId={}, event={}", userId, eventName);
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(jsonPayload, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                // 客户端断连（IOException/ClientAbortException）是正常现象，不需要打印 ERROR 堆栈
                if (isClientDisconnect(e)) {
                    log.debug("[SSE] 客户端已断连，清理 emitter: userId={}, event={}", userId, eventName);
                } else {
                    log.error("[SSE] 本地投递失败: userId={}, event={}", userId, eventName, e);
                }
                deadEmitters.add(emitter);
            }
        }

        deadEmitters.forEach(emitter -> removeEmitter(userId, emitter));
    }

    /**
     * 移除 SSE 连接
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    /**
     * 判断异常是否由客户端主动断连引起（IOException 及其子类，如 ClientAbortException）
     */
    private boolean isClientDisconnect(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof IOException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
