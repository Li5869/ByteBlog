package com.personblog.common.dto.Search;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章搜索同步DTO —— 用于从blog-article传递文章数据到blog-search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSearchDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String summary;

    private String cover;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    private String authorName;

    private String authorAvatar;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    private String categoryName;

    private List<String> tags;

    private Long views;

    private Long likes;

    private Long comments;

    private Long collections;

    private Boolean isTop;

    private Boolean isHot;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
