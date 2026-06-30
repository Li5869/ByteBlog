package com.personblog.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章润色结果
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章润色结果")
public class ArticlePolishVO {

    @Schema(description = "润色后的文章内容")
    private String polishedContent;

    @Schema(description = "润色说明")
    private String polishNote;
}
