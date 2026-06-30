package com.personblog.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知表
 *
 * @author LSH
 * @since 2026-04-07
 */
@Data
@TableName("tb_system_notification")
public class SystemNotification {

    /** 系统通知ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收通知的用户ID */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
