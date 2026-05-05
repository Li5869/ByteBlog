package com.personblog.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 专栏文章操作参数
 *
 * @author LSH
 */
@Data
@Schema(description = "专栏文章操作参数")
public class ColumnArticleDTO {

    @Schema(description = "文章ID列表")
    @NotEmpty(message = "文章ID列表不能为空")
    private List<Long> articleIds;
}
