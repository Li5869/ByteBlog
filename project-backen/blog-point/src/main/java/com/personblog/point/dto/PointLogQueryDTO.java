package com.personblog.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 积分流水查询参数 DTO
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@Schema(description = "积分流水查询参数")
public class PointLogQueryDTO {

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "积分类型筛选")
    private String type;
}
