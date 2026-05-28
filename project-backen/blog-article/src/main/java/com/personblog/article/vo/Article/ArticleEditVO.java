package com.personblog.article.vo.Article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 编辑文章回填数据
 *
 * @author LSH
 */
@Data
@Schema(description = "编辑文章回填数据")
public class ArticleEditVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章内容（Markdown）")
    private String content;

    @Schema(description = "封面URL")
    private String cover;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "状态：0-草稿，1-发布")
    private Integer status;
}
