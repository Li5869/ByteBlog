package com.personblog.article.dto.article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 文章发布/草稿保存参数
 *
 * @author LSH
 */
@Data
@Schema(description = "文章发布/草稿保存参数")
public class ArticlePublishDTO {

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

    @Schema(description = "自定义标签名称列表")
    private List<String> tagNames;

    @Schema(description = "状态：0-草稿，1-发布")
    private Integer status;

    /**
     * 写作任务ID（AI写作时关联）
     */
    @Schema(description = "写作任务ID（AI写作时关联，可选）")
    private Long taskId;
}
