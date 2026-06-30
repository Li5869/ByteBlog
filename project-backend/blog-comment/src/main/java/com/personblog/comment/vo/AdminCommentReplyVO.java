package com.personblog.comment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端评论回复VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端评论回复信息")
public class AdminCommentReplyVO {

    @Schema(description = "回复ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "回复者名称")
    private String authorName;

    @Schema(description = "回复者头像")
    private String authorAvatar;

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
