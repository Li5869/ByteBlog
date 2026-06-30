package com.personblog.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 管理端评论详情VO
 *
 * @author LSH
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端评论详情信息")
public class AdminCommentDetailVO extends AdminCommentVO {

    @Schema(description = "评论者邮箱")
    private String authorEmail;

    @Schema(description = "回复列表")
    private List<AdminCommentReplyVO> replies;
}
