package com.personblog.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 问题列表查询参数 DTO
 *
 * @author LSH
 */
@Data
@Schema(description = "问题列表查询参数")
public class QuestionQueryDTO {

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页数量")
    private Integer size = 10;

    @Schema(description = "筛选状态：all-全部，unanswered-待回答，solved-已解决，hot-热门")
    private String status = "all";

    @Schema(description = "排序方式：newest-最新，hot-最热")
    private String sortBy = "newest";

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "关键词搜索（标题/内容模糊匹配）")
    private String keyword;
}
