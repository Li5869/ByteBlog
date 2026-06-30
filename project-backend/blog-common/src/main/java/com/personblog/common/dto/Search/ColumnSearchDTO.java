package com.personblog.common.dto.Search;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 专栏搜索同步DTO —— 用于从blog-article传递专栏数据到blog-search
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnSearchDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String description;

    private String cover;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String authorName;

    private String authorAvatar;

    private Integer articlesCount;

    private Integer subscriptionCount;

    private Long views;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
