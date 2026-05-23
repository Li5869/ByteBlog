package com.personblog.common.dto.MqMessage.Comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentNotificationMessage {
    /** 文章 ID */
    private Long articleId;

    /** 父评论 ID（回复评论时不为 null） */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 当前创建的评论 ID */
    private Long commentId;

    /** 评论者用户 ID */
    private Long userId;

    /** 文章标题 */
    private String articleTitle;

    /** 评论数增减量：创建评论为 1，删除评论为 -N */
    private Integer delta;
}
