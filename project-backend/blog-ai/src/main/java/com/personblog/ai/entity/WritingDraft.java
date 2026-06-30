package com.personblog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI写作草稿表
 * 用户发布或存草稿后删除
 *
 * @author LSH
 */
@Data
@TableName("tb_writing_draft")
public class WritingDraft {

    /**
     * 草稿ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 关联写作任务ID（tb_writing_task.id）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

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
     * 分类ID（匹配已有分类时返回，否则为空）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    /**
     * 分类名称（AI生成，可能不在已有分类中）
     */
    private String categoryName;

    /**
     * 已有标签ID列表，逗号分隔（如 "1,2,3"）
     */
    private String tagIds;

    /**
     * 新标签名称列表，逗号分隔（需发布时自动创建）
     */
    private String tagNames;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
