package com.personblog.common.dto.Search;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作者搜索同步DTO —— 用于从blog-security传递用户数据到blog-search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorSearchDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String bio;

    private Long articlesCount;

    private Long fansCount;

    private Long likesCount;

    private Integer status;
}
