package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 管理端问题详情VO
 *
 * @author LSH
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端问题详情信息")
public class AdminQuestionDetailVO extends AdminQuestionVO {

    @Schema(description = "问题内容")
    private String content;

    @Schema(description = "标签列表")
    private List<AdminQuestionTagVO> tags;

    @Schema(description = "回答列表")
    private List<AdminAnswerVO> answers;
}
