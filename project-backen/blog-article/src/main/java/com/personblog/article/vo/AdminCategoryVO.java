package com.personblog.article.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端分类VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端分类详情")
public class AdminCategoryVO {

    @Schema(description = "分类ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "排序值（数字越小越靠前）")
    private Integer sort;

    @Schema(description = "该分类下的文章数")
    private Long articleCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
