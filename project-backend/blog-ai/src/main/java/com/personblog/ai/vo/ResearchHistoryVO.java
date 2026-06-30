package com.personblog.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 研究历史记录响应
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "研究历史记录响应")
public class ResearchHistoryVO {

    @Schema(description = "任务主键ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "任务UUID", example = "research_abc123")
    private String taskId;

    @Schema(description = "研究主题", example = "Redis分布式锁最佳实践")
    private String topic;

    @Schema(description = "任务状态", example = "completed")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "报告摘要（列表预览用）", example = "Redis分布式锁有多种实现方案...")
    private String reportSummary;
}
