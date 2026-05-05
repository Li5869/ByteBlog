package com.personblog.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Schema(description = "评论删除返回对象")
@Builder
public class CommentRemoveVO {
    @Schema(description = "删除的总数")
    private Integer deleteTotal;
}
