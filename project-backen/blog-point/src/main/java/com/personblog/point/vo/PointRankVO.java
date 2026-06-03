package com.personblog.point.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 积分排行榜 VO
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "积分排行榜")
public class PointRankVO {

    @Schema(description = "排行榜月份（格式：yyyy-MM）")
    private String yearMonth;

    @Schema(description = "参与排名的总用户数")
    private Integer totalUsers;

    @Schema(description = "当前用户排名")
    private Integer myRank;

    @Schema(description = "当前用户积分")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long myPoints;

    @Schema(description = "排行榜记录列表")
    private List<PointRankItemVO> records;
}
