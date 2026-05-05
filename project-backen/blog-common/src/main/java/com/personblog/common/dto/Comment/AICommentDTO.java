package com.personblog.common.dto.Comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICommentDTO {
    //文章id
    private Long articleId;
    //文章内容
    private String articleContent;
    //给作者发消息用
    private String articleTitle;
}
