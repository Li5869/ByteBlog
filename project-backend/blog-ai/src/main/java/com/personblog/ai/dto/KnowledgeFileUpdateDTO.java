package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库文件更新请求参数
 *
 * @author LSH
 */
@Data
@Schema(description = "知识库文件更新请求参数")
public class KnowledgeFileUpdateDTO {

    @Schema(description = "新文件名（可选）")
    private String fileName;

    @Schema(description = "新描述（可选）")
    private String description;
}
