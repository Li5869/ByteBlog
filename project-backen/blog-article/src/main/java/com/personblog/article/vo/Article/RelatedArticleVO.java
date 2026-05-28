package com.personblog.article.vo.Article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "相关文章返回对象")
public class RelatedArticleVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "封面图片URL")
    private String cover;
}
