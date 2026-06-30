package com.personblog.search.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleSearchVO {

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

    private LocalDateTime createdAt;

    private List<String> highlightTitle;

    private List<String> highlightSummary;
}