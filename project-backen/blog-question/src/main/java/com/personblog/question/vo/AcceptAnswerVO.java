package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "采纳最佳答案返回对象")
public class AcceptAnswerVO {

    @Schema(description = "问题ID")
    private Long questionId;

    @Schema(description = "被采纳的回答ID")
    private Long answerId;

    @Schema(description = "问题是否已解决")
    private Boolean isSolved;
}
