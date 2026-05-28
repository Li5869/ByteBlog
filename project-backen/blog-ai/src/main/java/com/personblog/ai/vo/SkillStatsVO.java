package com.personblog.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skill 统计信息 VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Skill 统计信息")
public class SkillStatsVO {

    @Schema(description = "Skill 总数")
    private Integer totalSkills;

    @Schema(description = "切片总数")
    private Integer totalChunks;
}