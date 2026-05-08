package com.personblog.ai.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "发送消息请求")
public class ChatMessageDTO {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容")
    private String content;
}
