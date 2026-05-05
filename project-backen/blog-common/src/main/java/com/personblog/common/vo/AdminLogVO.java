package com.personblog.common.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端操作日志VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端操作日志详情")
public class AdminLogVO {

    @Schema(description = "日志ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "操作管理员ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long adminId;

    @Schema(description = "管理员名称")
    private String adminName;

    @Schema(description = "操作类型")
    private String actionType;

    @Schema(description = "操作对象类型")
    private String targetType;

    @Schema(description = "操作对象ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    @Schema(description = "操作描述")
    private String description;

    @Schema(description = "操作详情")
    private String actionDetail;

    @Schema(description = "操作IP地址")
    private String ipAddress;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
