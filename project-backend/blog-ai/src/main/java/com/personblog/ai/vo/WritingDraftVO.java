package com.personblog.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "写作成果展示")
public class WritingDraftVO {
    /**
     * AI生成的文章标题
     */
    private String title;

    /**
     * AI生成的文章摘要
     */
    private String summary;

    /**
     * AI生成的文章正文（Markdown）
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String cover;

    /**
     * 分类名称（AI生成，可能不在已有分类中）
     */
    private String categoryName;
    /**
     * 分类ID（匹配已有分类时返回，否则为空）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    /**
     * 已有标签ID列表，逗号分隔（如 "1,2,3"）
     */
    private String tagIds;

    /**
     * 新标签名称列表，逗号分隔（需发布时自动创建）
     */
    private String tagNames;

    /**
     * 全部标签名称，用于前端展示
     */
    private List<String> allTagNames;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 综合评分（0.0-10.0，加权计算）
     */
    private BigDecimal score;

    /**
     * 完整性评分（权重30%，0.0-10.0）
     */
    private BigDecimal completeness;

    /**
     * 结构性评分（权重20%，0.0-10.0）
     */
    private BigDecimal structure;

    /**
     * 表达质量评分（权重25%，0.0-10.0）
     */
    private BigDecimal expression;

    /**
     * 实用性评分（权重15%，0.0-10.0）
     */
    private BigDecimal practicality;

    /**
     * 格式规范评分（权重10%，0.0-10.0）
     */
    private BigDecimal format;

    /**
     * 优点列表（JSON数组，如 ["优点1","优点2"]）
     */
    private String strengths;

    /**
     * 不足列表（JSON数组，如 ["不足1","不足2"]）
     */
    private String weaknesses;

    /**
     * 改进建议列表（JSON数组，如 ["建议1","建议2"]）
     */
    private String suggestions;
}
