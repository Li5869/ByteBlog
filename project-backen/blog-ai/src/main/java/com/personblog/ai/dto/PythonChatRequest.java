package com.personblog.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonChatRequest {

    @JsonProperty("conversation_id")
    private String conversationId;

    private String message;

    @JsonProperty("deep_thinking")
    private Boolean deepThinking;

    /**
     * 当前用户ID（由 Java 后端从 UserContextHolder 获取并传递）
     * Python 端可根据此 ID 提供个性化服务。
     * 使用 String 类型避免 Long → JSON 数字的精度丢失问题，
     * 同时确保 Python 端接收到的始终是字符串格式。
     */
    @JsonProperty("user_id")
    private String userId;
}
