package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库文件列表查询参数
 *
 * @author LSH
 */
@Data
@Schema(description = "知识库文件列表查询参数")
public class KnowledgeFileQueryDTO {

    @Schema(description = "当前页码，默认 1")
    private Integer current = 1;

    @Schema(description = "每页条数，默认 10，最大 50")
    private Integer size = 10;

    @Schema(description = "搜索关键字（按文件名模糊搜索）")
    private String keyword;

    @Schema(description = "按来源筛选：file_upload / article_sync")
    private String source;
}
