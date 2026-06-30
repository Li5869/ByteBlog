package com.personblog.point.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分流水 VO
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "积分流水")
public class PointLogVO {

    @Schema(description = "记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "积分变动值（正数增加，负数减少）")
    private Integer points;

    @Schema(description = "积分类型")
    private String type;

    @Schema(description = "描述信息")
    private String description;

    @Schema(description = "业务ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
