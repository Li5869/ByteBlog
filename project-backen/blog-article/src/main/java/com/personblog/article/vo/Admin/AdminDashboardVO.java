package com.personblog.article.vo.Admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端仪表盘概览统计VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端仪表盘概览统计")
public class AdminDashboardVO {

    @Schema(description = "文章总数")
    private Long articlesTotal;

    @Schema(description = "文章数月环比变化")
    private String articlesChange;

    @Schema(description = "用户总数")
    private Long usersTotal;

    @Schema(description = "用户数月环比变化")
    private String usersChange;

    @Schema(description = "评论总数")
    private Long commentsTotal;

    @Schema(description = "评论数月环比变化")
    private String commentsChange;

    @Schema(description = "问答总数")
    private Long questionsTotal;

    @Schema(description = "问答数月环比变化")
    private String questionsChange;
}
