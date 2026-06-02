package com.personblog.point.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分余额 VO
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "积分余额")
public class PointBalanceVO {

    @Schema(description = "累计积分")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long totalPoints;

    @Schema(description = "可用积分")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long availablePoints;

    @Schema(description = "今日获得积分")
    private Integer todayEarned;

    @Schema(description = "当前排名（月度）")
    private Integer rank;
}
