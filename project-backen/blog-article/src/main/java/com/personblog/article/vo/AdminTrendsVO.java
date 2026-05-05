package com.personblog.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理端趋势数据VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端趋势数据")
public class AdminTrendsVO {

    @Schema(description = "月份标签")
    private List<String> months;

    @Schema(description = "每月文章新增数")
    private List<Long> articles;

    @Schema(description = "每月用户新增数")
    private List<Long> users;

    @Schema(description = "每月评论新增数")
    private List<Long> comments;

    @Schema(description = "每月问答新增数")
    private List<Long> questions;
}
