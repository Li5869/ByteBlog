package com.personblog.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文件删除响应 VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库文件删除响应")
public class KnowledgeFileDeleteVO {

    @Schema(description = "删除的文件ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    @Schema(description = "删除的 Parent Chunk 数量")
    private Integer deletedParentCount;

    @Schema(description = "删除的 Child 向量数量")
    private Integer deletedChildCount;
}
