package com.personblog.common.dto.MqMessage.notifaction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知推送消息")
public class NotificationMessage {
    
    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "接收通知的用户ID")
    private Long userId;
    
    @Schema(description = "行为类型：like/comment/reply/follow/collection/answer")
    private String actionType;
    
    @Schema(description = "目标类型：article/comment/question/answer/user")
    private String targetType;
    
    @Schema(description = "目标ID")
    private Long targetId;
    
    @Schema(description = "发送者ID")
    private Long senderId;
    
    @Schema(description = "发送者昵称")
    private String senderNickname;
    
    @Schema(description = "发送者头像")
    private String senderAvatar;
    
    @Schema(description = "目标标题（如文章标题）")
    private String targetTitle;
    
    @Schema(description = "内容（评论/回复内容）")
    private String content;
    
    @Schema(description = "关联的文章ID（当targetType为comment/answer时，用于跳转定位）")
    private Long relatedId;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}