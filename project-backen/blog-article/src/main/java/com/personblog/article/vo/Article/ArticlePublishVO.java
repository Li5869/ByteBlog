package com.personblog.article.vo.Article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章发布结果
 *
 * @author LSH
 */
@Data
@Schema(description = "文章发布结果")
public class ArticlePublishVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章状态：0-草稿，1-发布")
    private Integer status;
}
