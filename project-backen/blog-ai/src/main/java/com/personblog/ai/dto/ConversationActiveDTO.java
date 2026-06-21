package com.personblog.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话活跃标记 DTO — 从 Redis chat:active:* 解析出的对话信息
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationActiveDTO {

    /** 会话ID（对应 Redis Key 中 conversation_id 部分） */
    private String conversationId;

    /** 用户ID */
    private String userId;
}
