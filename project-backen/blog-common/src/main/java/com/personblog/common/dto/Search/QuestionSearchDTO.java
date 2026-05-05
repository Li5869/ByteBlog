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
 * 问题搜索同步DTO —— 用于从blog-question传递问题数据到blog-search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSearchDTO {

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

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
