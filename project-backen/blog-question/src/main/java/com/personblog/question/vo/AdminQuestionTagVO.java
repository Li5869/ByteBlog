package com.personblog.question.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端问题标签VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端问题标签信息")
public class AdminQuestionTagVO {

    @Schema(description = "标签ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "标签名称")
    private String name;
}
