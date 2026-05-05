package com.personblog.common.dto.Column;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 专栏查询参数
 *
 * @author LSH
 */
@Data
@Schema(description = "专栏查询参数")
public class ColumnQueryDTO {

    @Schema(description = "当前页码，默认1")
    private Integer current = 1;

    @Schema(description = "每页数量，默认10")
    private Integer size = 10;

    @Schema(description = "用户ID(查看指定用户的专栏)")
    private Long userId;

    @Schema(description = "状态筛选：0-草稿，1-已发布")
    private Integer status;

    @Schema(description = "排序字段：created_at-按时间，articles_count-按文章数，views-按浏览量")
    private String orderBy;

    @Schema(description = "关键词搜索(标题)")
    private String keyword;
}
