package com.personblog.push.service;

import com.personblog.push.constant.PushConstants;
import com.personblog.push.onlineMessage.PushMessage;
import com.personblog.push.sse.SseEmitterManager;
import com.personblog.push.websocket.WebSocketHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 跨节点消息推送服务
 * 基于 Redis Pub/Sub 实现多实例部署下的实时消息分发
 *
 * @author LSH
 */
@Slf4j
@Service
public class PushChannelService {

    private final RedissonClient redissonClient;
    private final WebSocketHandler webSocketHandler;
    private final SseEmitterManager sseEmitterManager;

    public PushChannelService(RedissonClient redissonClient,
                              @Lazy WebSocketHandler webSocketHandler,
                              @Lazy SseEmitterManager sseEmitterManager) {
        this.redissonClient = redissonClient;
        this.webSocketHandler = webSocketHandler;
        this.sseEmitterManager = sseEmitterManager;
    }

    @PostConstruct
    public void subscribe() {
        RTopic topic = redissonClient.getTopic(PushConstants.TOPIC_NAME);
        topic.addListener(PushMessage.class, (channel, msg) -> {
            try {
                handleMessage(msg);
            } catch (Exception e) {
                log.error("[PushChannel] 处理跨节点消息失败: channel={}, userId={}", msg.getChannel(), msg.getUserId(), e);
            }
        });
        log.info("[PushChannel] 订阅 Redis Topic: {}", PushConstants.TOPIC_NAME);
    }

    /**
     * 发布消息到 Redis，所有节点（含自身）都会收到并尝试本地投递
     */
    public void publish(PushMessage msg) {
        RTopic topic = redissonClient.getTopic(PushConstants.TOPIC_NAME);
        topic.publish(msg);
    }

    private void handleMessage(PushMessage msg) {
        switch (msg.getChannel()) {
            case PushConstants.CHANNEL_WS -> webSocketHandler.sendToLocalUser(msg.getUserId(), msg.getPayload());
            case PushConstants.CHANNEL_SSE -> {
                String eventName = msg.getEventName() != null ? msg.getEventName() : "notification";
                sseEmitterManager.sendToLocalUser(msg.getUserId(), msg.getPayload(), eventName);
            }
            case PushConstants.CHANNEL_SSE_BROADCAST -> handleSseBroadcast(msg);
            default -> log.warn("[PushChannel] 未知 channel 类型: {}", msg.getChannel());
        }
    }

    /**
     * SSE 粉丝广播：遍历 followerIds，对本节点有 SSE 连接的粉丝投递在线状态事件
     */
    private void handleSseBroadcast(PushMessage msg) {
        List<Long> followerIds = msg.getFollowerIds();
        if (followerIds == null || followerIds.isEmpty()) {
            return;
        }
        log.info("[PushChannel] SSE广播: userId={}, 粉丝数={}, followerIds={}", msg.getUserId(), followerIds.size(), followerIds);
        for (Long followerId : followerIds) {
            try {
                sseEmitterManager.sendToLocalUser(followerId, msg.getPayload(), "user_online");
            } catch (Exception e) {
                log.error("[PushChannel] SSE广播投递失败: followerId={}", followerId, e);
            }
        }
    }
}
