package com.personblog.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务通知表
 *
 * @author LSH
 * @since 2026-04-07
 */
@Data
@TableName("tb_biz_notification")
public class BizNotification {

    /** 业务通知ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收通知的用户ID（被点赞/评论/关注的人） */
    private Long userId;

    /** 行为类型: like-点赞, comment-评论, reply-回复, follow-关注, collection-收藏, answer-回答,adopt-采纳 */
    private String actionType;

    /** 目标类型: article-文章, comment-评论, question-问题, answer-回答, user-用户 */
    private String targetType;

    /** 目标ID（文章ID/评论ID/问题ID/回答ID/用户ID） */
    private Long targetId;

    /** 发送者ID（触发通知的用户） */
    private Long senderId;

    /**目标标题(若有)*/
    private String targetTitle;

    /**内容(reply,comment,answer有)*/
    private String content;

    /** 关联的内容ID（当targetType为comment/answer时，用于跳转定位） */
    private Long relatedId;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
