package com.personblog.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "评论返回对象")
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "评论者信息")
    private AuthorInfo author;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞数")
    private Long likes;

    @Schema(description = "评论时间")
    private LocalDateTime createdAt;

    @Schema(description = "回复列表")
    private List<CommentVO> replies;
    
    @Schema(description = "评论总数")
    private Long commentTotal;
    
    @Data
    @Schema(description = "评论者信息")
    public static class AuthorInfo {
        @Schema(description = "用户ID")
        private Long id;

        @Schema(description = "用户昵称")
        private String name;

        @Schema(description = "用户头像")
        private String avatar;
    }
}
