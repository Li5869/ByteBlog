package com.personblog.push.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PushMessageVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    private String senderName;

    private String senderAvatar;

    private String content;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;

    private LocalDateTime createdAt;

    private Boolean hasImage;
}
