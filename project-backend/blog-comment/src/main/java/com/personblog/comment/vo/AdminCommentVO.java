package com.personblog.comment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 管理端评论列表VO
 *
 * @author LSH
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端评论列表信息")
public class AdminCommentVO {

    @Schema(description = "评论ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "评论者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    @Schema(description = "评论者名称")
    private String authorName;

    @Schema(description = "评论者头像")
    private String authorAvatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论对象类型：article")
    private String targetType;

    @Schema(description = "评论对象ID（文章ID）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    @Schema(description = "评论对象标题")
    private String targetTitle;

    @Schema(description = "审核状态：pending/approved/rejected")
    private String status;

    @Schema(description = "点赞数")
    private Long likes;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;
}
