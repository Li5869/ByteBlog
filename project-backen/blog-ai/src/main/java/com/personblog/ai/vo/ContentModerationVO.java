package com.personblog.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内容审核结果
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "内容审核结果")
public class ContentModerationVO {

    @Schema(description = "是否违规")
    private Boolean isViolation;

    @Schema(description = "违规原因和建议")
    private String reason;

}