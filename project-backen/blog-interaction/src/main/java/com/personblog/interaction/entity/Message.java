package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信消息表
 *
 * @author LSH
 * @since 2026-04-07
 */
@Data
@TableName("tb_message")
public class Message {

    /** 消息ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 发送者ID，逻辑外键关联tb_user */
    private Long senderId;

    /** 接收者ID，逻辑外键关联tb_user */
    private Long receiverId;

    /** 消息内容 */
    private String content;

    /** 是否已读: false-未读, true-已读 */
    private Boolean isRead;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
