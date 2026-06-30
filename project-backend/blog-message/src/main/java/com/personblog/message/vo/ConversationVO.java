package com.personblog.message.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.personblog.common.dto.Notification.SenderVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "会话视图对象")
public class ConversationVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "对方用户信息")
    private SenderVO user;

    @Schema(description = "最后一条消息内容")
    private String lastMessage;

    @Schema(description = "未读消息数")
    private Integer unreadCount;

    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
