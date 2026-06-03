package com.personblog.point.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签到结果 VO
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "签到结果")
public class SignResultVO {

    @Schema(description = "签到是否成功")
    private Boolean success;

    @Schema(description = "本次获得积分（基础+额外）")
    private Integer points;

    @Schema(description = "连续签到天数")
    private Integer continuousDays;

    @Schema(description = "本月累计签到天数")
    private Integer totalSignDays;

    @Schema(description = "连续签到额外奖励积分")
    private Integer extraPoints;
}
