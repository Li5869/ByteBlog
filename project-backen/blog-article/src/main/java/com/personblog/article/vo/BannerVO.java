package com.personblog.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "轮播图返回对象")
public class BannerVO {

    @Schema(description = "轮播图ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "封面图片URL")
    private String cover;

    @Schema(description = "关联文章ID")
    private Long articleId;
}
