package com.personblog.article.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端最近文章VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端最近文章")
public class AdminRecentArticleVO {

    @Schema(description = "文章ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "状态：published/draft")
    private String status;

    @Schema(description = "浏览量")
    private Long views;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
