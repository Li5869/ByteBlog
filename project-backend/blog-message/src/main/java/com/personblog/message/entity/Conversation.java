package com.personblog.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_conversation")
public class Conversation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long targetUserId;

    private String lastMessage;

    private LocalDateTime lastMessageTime;

    private Integer unreadCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
