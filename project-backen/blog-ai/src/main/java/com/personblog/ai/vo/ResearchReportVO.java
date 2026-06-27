package com.personblog.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 研究报告响应
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "研究报告响应")
public class ResearchReportVO {

    @Schema(description = "研究主题", example = "Redis分布式锁最佳实践")
    private String topic;

    @Schema(description = "OSS报告文件地址（Markdown格式）", example = "https://oss.example.com/reports/research_abc123.md")
    private String reportUrl;

    @Schema(description = "报告摘要", example = "Redis分布式锁有多种实现方案...")
    private String summary;

    @Schema(description = "关键发现列表")
    private List<String> keyFindings;

    @Schema(description = "引用来源列表")
    private List<SourceItem> sources;

    /**
     * 引用来源项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "引用来源项")
    public static class SourceItem {

        @Schema(description = "来源标题", example = "Redisson官方文档")
        private String title;

        @Schema(description = "来源URL", example = "https://redisson.org/docs/")
        private String url;
    }
}
