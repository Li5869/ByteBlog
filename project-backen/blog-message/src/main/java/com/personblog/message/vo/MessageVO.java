package com.personblog.message.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "消息视图对象")
public class MessageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "消息ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "发送者ID")
    private Long senderId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "接收者ID")
    private Long receiverId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型：sent-发送的消息，received-接收的消息")
    private String type;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
