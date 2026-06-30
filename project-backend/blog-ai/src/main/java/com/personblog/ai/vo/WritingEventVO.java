package com.personblog.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 写作事件VO
 * 
 * 用于SSE流式返回写作进度事件
 * 
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "写作事件")
public class WritingEventVO {

    @Schema(description = "事件类型", example = "phase")
    private String type;

    @Schema(description = "事件数据")
    private Object data;

    public static final WritingEventVO DONE_EVENT = WritingEventVO.builder()
            .type("done")
            .data(null)
            .build();
}
