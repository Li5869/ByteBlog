package com.personblog.search.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Data
@Document(indexName = "author")
@Setting(settingPath = "elasticsearch/author-setting.json")
public class AuthorDocument {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String nickname;

    @Field(type = FieldType.Keyword)
    private String avatar;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String bio;

    @Field(type = FieldType.Long)
    private Long articlesCount;

    @Field(type = FieldType.Long)
    private Long fansCount;

    @Field(type = FieldType.Long)
    private Long likesCount;

    @Field(type = FieldType.Integer)
    private Integer status;

    @CompletionField(maxInputLength = 30)
    private Completion nicknameSuggest;
}