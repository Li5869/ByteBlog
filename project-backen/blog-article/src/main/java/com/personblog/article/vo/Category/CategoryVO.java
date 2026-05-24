package com.personblog.article.vo.Category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分类返回对象")
public class CategoryVO {

    /** 分类ID */
    @Schema(description = "分类ID")
    private Long id;

    /** 分类名称 */
    @Schema(description = "分类名称")
    private String name;

    /** 排序(数字越小越靠前) */
    @Schema(description = "排序(数字越小越靠前)")
    private Integer sort;

    /** 文章数量 */
    @Schema(description = "文章数量")
    private Long articlesCount;
}
