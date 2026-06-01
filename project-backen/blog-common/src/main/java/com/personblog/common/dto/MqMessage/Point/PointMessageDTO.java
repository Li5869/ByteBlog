package com.personblog.common.dto.MqMessage.Point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分消息 DTO
 * 用于 MQ 传递积分变动信息
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointMessageDTO {

    /** 用户ID */
    private Long userId;

    /** 积分变动值（正数增加，负数减少） */
    private Integer points;

    /** 积分类型: sign/article_published/article_liked/article_collected/comment_liked/answer_liked/admin_adjust */
    private String type;

    /** 业务ID（文章ID、评论ID等，可为null） */
    private Long bizId;

    /** 管理员调整时的描述 */
    private String description;

    /** 操作人ID（管理员调整时使用） */
    private Long operatorId;

    /** 消息创建时间 */
    private LocalDateTime createTime;
}
