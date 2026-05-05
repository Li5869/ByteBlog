package com.personblog.question.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 管理端问题列表VO
 *
 * @author LSH
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端问题列表信息")
public class AdminQuestionVO {

    @Schema(description = "问题ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "问题标题")
    private String title;

    @Schema(description = "提问者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    @Schema(description = "提问者名称")
    private String authorName;

    @Schema(description = "提问者头像")
    private String authorAvatar;

    @Schema(description = "回答数")
    private Long answerCount;

    @Schema(description = "浏览量")
    private Long viewCount;

    @Schema(description = "状态：pending/solved")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
