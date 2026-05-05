package com.personblog.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话详情")
public class ConversationDetailVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "会话模式")
    private String mode;

    @Schema(description = "消息总数")
    private Integer messageCount;

    @Schema(description = "消息列表")
    private List<MessageVO> messages;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
