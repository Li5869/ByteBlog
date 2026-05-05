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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息详情")
public class MessageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "角色：user/assistant")
    private String role;

    @Schema(description = "思考过程")
    private String thinking;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "工具调用列表")
    private List<ToolCallVO> toolCalls;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "工具调用信息")
    public static class ToolCallVO {
        
        @Schema(description = "工具调用ID")
        private Integer id;

        @Schema(description = "工具名称")
        private String name;

        @Schema(description = "工具参数")
        private String args;
    }
}
