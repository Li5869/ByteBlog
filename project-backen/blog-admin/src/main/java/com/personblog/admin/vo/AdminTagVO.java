package com.personblog.admin.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端标签VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端标签详情")
public class AdminTagVO {

    @Schema(description = "标签ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "使用次数（被多少篇文章使用）")
    private Long usageCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
