package com.personblog.search.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;


@Data
public class AuthorSearchVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String bio;

    private Long articlesCount;

    private Long fansCount;

    private Long likesCount;

    private List<String> highlightNickname;
}