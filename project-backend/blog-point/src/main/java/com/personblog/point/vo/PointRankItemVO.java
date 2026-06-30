package com.personblog.point.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分排行榜项 VO
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "积分排行榜项")
public class PointRankItemVO {

    @Schema(description = "排名")
    private Integer rank;

    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像URL")
    private String avatar;

    @Schema(description = "积分")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long points;
}
