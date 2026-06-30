package com.personblog.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文件上传响应 VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库文件上传响应")
public class KnowledgeFileUploadVO {

    @Schema(description = "文件记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "源文件访问地址")
    private String fileUrl;

    @Schema(description = "Parent Chunk 数量")
    private Integer chunkCount;
}
