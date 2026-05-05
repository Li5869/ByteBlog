package com.personblog.interaction.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "业务通知列表项")
public class BizNotificationVO {

    @Schema(description = "通知ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "接收用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "行为类型：like/comment/reply/follow/collection/answer")
    private String actionType;

    @Schema(description = "目标类型：article/comment/question/answer/user")
    private String targetType;

    @Schema(description = "目标ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    @Schema(description = "发送者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    @Schema(description = "发送者信息")
    private SenderVO sender;

    @Schema(description = "目标标题（如文章标题）")
    private String targetTitle;

    @Schema(description = "评论/回复内容（仅 comment/reply/answer 类型有值）")
    private String content;

    @Schema(description = "关联的内容ID（当targetType为comment/answer时，用于跳转定位）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long relatedId;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
