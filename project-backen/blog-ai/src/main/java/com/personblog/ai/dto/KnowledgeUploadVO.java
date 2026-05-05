package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库上传响应")
public class KnowledgeUploadVO {

    @Schema(description = "文本块数量")
    private Integer chunkCount;

    @Schema(description = "文件名（文件上传时返回）")
    private String filename;

    @Schema(description = "文本块ID列表")
    private java.util.List<String> ids;
}
