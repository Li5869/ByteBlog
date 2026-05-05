package com.personblog.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SseEmitterManager {
    // 存储用户ID与SseEmitter的映射（支持一个用户多个连接）
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /**
     * 创建 SSE 连接
     * @param userId 用户ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        // 创建 SseEmitter，设置超时时间（0表示永不超时）
        SseEmitter emitter = new SseEmitter(0L);

        // 注册回调
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        // 添加到集合
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        return emitter;
    }

    /**
     * 推送消息给指定用户
     * @param userId 用户ID
     * @param message 消息内容
     */
    public void sendToUser(Long userId, Object message) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("用户 {} 没有 SSE 连接，无法推送消息", userId);
            return;
        }

        // 遍历该用户的所有连接（多标签页）
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(message, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.error("推送消息失败: userId={}", userId, e);
                deadEmitters.add(emitter);
            }
        }

        // 移除失效的连接
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
}