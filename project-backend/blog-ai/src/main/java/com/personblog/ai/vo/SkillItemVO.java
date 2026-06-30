package com.personblog.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skill 单项信息 VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Skill 单项信息")
public class SkillItemVO {

    @Schema(description = "Skill 名称")
    private String name;

    @Schema(description = "切片数量")
    private Integer chunkCount;

    @Schema(description = "状态：indexed / empty")
    private String status;
}