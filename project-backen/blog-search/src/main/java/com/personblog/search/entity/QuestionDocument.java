package com.personblog.search.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(indexName = "question")
@Setting(settingPath = "elasticsearch/question-setting.json")
public class QuestionDocument {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Long)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    @Field(type = FieldType.Keyword)
    private String authorName;

    @Field(type = FieldType.Keyword)
    private String authorAvatar;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Long)
    private Long views;

    @Field(type = FieldType.Long)
    private Long answers;

    @Field(type = FieldType.Long)
    private Long likes;

    @Field(type = FieldType.Boolean)
    private Boolean isSolved;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;

    @CompletionField(maxInputLength = 50)
    private Completion titleSuggest;
}