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
    public  class PushMessage {
        /** 消息通道类型：ws / ws:broadcast / sse */
        private String channel;
        /** 目标用户ID（单播时使用） */
        private Long userId;
        /** 消息 JSON 内容 */
        private String payload;
        /** 粉丝广播时的目标用户ID列表（仅 ws:broadcast 使用） */
        private List<Long> followerIds;
    }