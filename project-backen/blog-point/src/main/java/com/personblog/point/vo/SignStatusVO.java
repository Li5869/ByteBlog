package com.personblog.point.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 签到状态 VO
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "签到状态")
public class SignStatusVO {

    @Schema(description = "今日是否已签到")
    private Boolean signed;

    @Schema(description = "连续签到天数")
    private Integer continuousDays;

    @Schema(description = "本月累计签到天数")
    private Integer totalSignDays;

    @Schema(description = "当月已签到的日期列表（1-31）")
    private List<Integer> signCalendar;
}
