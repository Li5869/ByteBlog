package com.personblog.search.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
    public class QuestionSearchVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String content;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    private String authorName;

    private String authorAvatar;

    private List<String> tags;

    private Long views;

    private Long answers;

    private Long likes;

    private Boolean isSolved;

    private LocalDateTime createdAt;

    private List<String> highlightTitle;

    private List<String> highlightContent;
}