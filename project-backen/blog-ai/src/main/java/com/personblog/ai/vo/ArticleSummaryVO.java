package com.personblog.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章摘要生成结果
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章摘要生成结果")
public class ArticleSummaryVO {

    @Schema(description = "生成的摘要")
    private String summary;
}