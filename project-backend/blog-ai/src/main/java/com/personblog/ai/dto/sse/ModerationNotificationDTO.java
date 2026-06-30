package com.personblog.ai.dto.sse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审核通知消息DTO
 * 用于SSE推送审核结果
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "审核通知消息")
public class ModerationNotificationDTO {

    @Schema(description = "消息类型：moderation")
    private String type;

    @Schema(description = "业务类型：article/comment/question")
    private String bizType;

    @Schema(description = "业务ID")
    private Long bizId;

    @Schema(description = "审核状态：approved/rejected")
    private String reviewStatus;

    @Schema(description = "内容标题")
    private String title;

    @Schema(description = "审核未通过原因")
    private String reason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
