package com.personblog.question.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端回答VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端回答信息")
public class AdminAnswerVO {

    @Schema(description = "回答ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "回答者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    @Schema(description = "回答者名称")
    private String authorName;

    @Schema(description = "回答者头像")
    private String authorAvatar;

    @Schema(description = "回答内容")
    private String content;

    @Schema(description = "点赞数")
    private Long likeCount;

    @Schema(description = "是否最佳答案")
    private Boolean isBest;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
