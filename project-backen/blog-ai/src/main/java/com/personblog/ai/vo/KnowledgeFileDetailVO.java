package com.personblog.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文件详情响应 VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库文件详情响应")
public class KnowledgeFileDetailVO {

    @Schema(description = "文件ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件描述")
    private String description;

    @Schema(description = "源文件访问地址")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "Parent Chunk 数量")
    private Integer chunkCount;

    @Schema(description = "关联的 Parent Chunk ID 列表")
    private List<String> parentIds;

    @Schema(description = "来源：file_upload / article_sync")
    private String source;

    @Schema(description = "上传者用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long uploaderId;

    @Schema(description = "上传者用户名")
    private String uploaderName;

    @Schema(description = "上传时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
