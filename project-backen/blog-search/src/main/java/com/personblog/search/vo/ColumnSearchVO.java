package com.personblog.search.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 专栏搜索结果 VO
 *
 * @author LSH
 */
@Data
public class ColumnSearchVO {

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

    private LocalDateTime createdAt;

    /** 标题高亮片段 */
    private List<String> highlightTitle;

    /** 描述高亮片段 */
    private List<String> highlightDescription;
}
