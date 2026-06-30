package com.personblog.push.websocket;

import com.personblog.common.utils.MessageUtil;
import com.personblog.push.constant.PushConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.personblog.push.constant.PushConstants.TYPE_USER_ONLINE;

/**
 * WebSocket 消息实体
 *
 * @author LSH
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    private String type;
    private Object data;
    private Long timestamp;

    public WebSocketMessage(String type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 私信消息推送
     *
     * @param senderId 发送者ID
     * @param senderName 发送者昵称
     * @param senderAvatar 发送者头像
     * @param content 消息内容
     * @param messageId 消息ID
     * @param createdAt 创建时间
     */
    public static WebSocketMessage privateMessage(
            Long senderId,
            String senderName,
            String senderAvatar,
            String content,
            Long messageId,
            LocalDateTime createdAt
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", String.valueOf(senderId));  // 转为字符串，避免JS精度丢失
        data.put("senderName", senderName);
        data.put("senderAvatar", senderAvatar);
        data.put("content", MessageUtil.truncateContent(content));
        data.put("hasImage", MessageUtil.containsImage(content));
        data.put("messageId", String.valueOf(messageId));  // 转为字符串，避免JS精度丢失
        data.put("createdAt", createdAt.toString());
        data.put("timestamp", System.currentTimeMillis());

        return new WebSocketMessage(PushConstants.TYPE_PRIVATE_MESSAGE, data);
    }
    /**
     * 欢迎消息
     */
    public static WebSocketMessage welcome(Long userId) {
        return new WebSocketMessage(PushConstants.TYPE_WELCOME, Map.of(
                "userId", String.valueOf(userId),  // 转为字符串，避免JS精度丢失
                "message", "连接成功",
                "serverTime", System.currentTimeMillis()
        ));
    }

    /**
     * 用户上线通知
     */
    public static WebSocketMessage userOnline(Long userId) {
        return new WebSocketMessage(TYPE_USER_ONLINE, Map.of(
                "userId", String.valueOf(userId),  // 转为字符串，避免JS精度丢失
                "online", true
        ));
    }

    /**
     * 用户下线通知
     */
    public static WebSocketMessage userOffline(Long userId) {
        return new WebSocketMessage(TYPE_USER_ONLINE, Map.of(
                "userId", String.valueOf(userId),  // 转为字符串，避免JS精度丢失
                "online", false
        ));
    }

    /**
     * 在线状态查询结果
     */
    public static WebSocketMessage onlineStatus(Map<Long, Boolean> status) {
        // 将所有 Long 类型的 key 转为 String，避免JS精度丢失
        Map<String, Boolean> stringKeyStatus = new HashMap<>();
        status.forEach((key, value) -> stringKeyStatus.put(String.valueOf(key), value));
        return new WebSocketMessage(PushConstants.TYPE_ONLINE_STATUS, stringKeyStatus);
    }

    /**
     * 错误消息
     */
    public static WebSocketMessage error(String message) {
        return new WebSocketMessage(PushConstants.TYPE_ERROR, Map.of("message", message));
    }

    public static WebSocketMessage unreadUpdate(Long receiverId, int delta) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", String.valueOf(receiverId));  // 转为字符串，避免JS精度丢失
        payload.put("delta", delta);
        return new WebSocketMessage(PushConstants.TYPE_UNREAD_UPDATE, payload);
    }
}
