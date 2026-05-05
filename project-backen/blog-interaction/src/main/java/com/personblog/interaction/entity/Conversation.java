package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信会话表
 *
 * @author LSH
 * @since 2026-04-07
 */
@Data
@TableName("tb_conversation")
public class Conversation {

    /** 会话ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID，逻辑外键关联tb_user */
    private Long userId;

    /** 目标用户ID（对话的另一方），逻辑外键关联tb_user */
    private Long targetUserId;

    /** 最后一条消息内容 */
    private String lastMessage;

    /** 最后一条消息时间 */
    private LocalDateTime lastMessageTime;

    /** 未读消息数 */
    private Integer unreadCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
