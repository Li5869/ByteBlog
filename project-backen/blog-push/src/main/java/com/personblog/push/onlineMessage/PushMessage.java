package com.personblog.push.onlineMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 跨节点推送消息载体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {
    /** 消息通道类型：ws / sse / sse:broadcast */
    private String channel;
    /** 目标用户ID（单播时使用） */
    private Long userId;
    /** 消息 JSON 内容 */
    private String payload;
    /** 粉丝广播时的目标用户ID列表（仅 sse:broadcast 使用） */
    private List<Long> followerIds;
    /** SSE 事件名（默认 notification，可自定义如 user_online / unread_update） */
    private String eventName;

    public PushMessage(String channel, Long userId, String payload, List<Long> followerIds) {
        this.channel = channel;
        this.userId = userId;
        this.payload = payload;
        this.followerIds = followerIds;
    }
}
