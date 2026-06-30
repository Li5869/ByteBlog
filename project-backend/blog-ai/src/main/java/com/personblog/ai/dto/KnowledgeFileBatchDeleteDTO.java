package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 知识库文件批量删除请求参数
 *
 * @author LSH
 */
@Data
@Schema(description = "知识库文件批量删除请求参数")
public class KnowledgeFileBatchDeleteDTO {

    @Schema(description = "文件ID列表")
    private List<Long> fileIds;
}
