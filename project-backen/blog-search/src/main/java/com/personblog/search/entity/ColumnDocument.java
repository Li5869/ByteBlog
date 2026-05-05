package com.personblog.search.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.time.LocalDateTime;

/**
 * 专栏 ES 文档实体
 * 对应 Elasticsearch 中的 column 索引
 *
 * @author LSH
 */
@Data
@Document(indexName = "column")
@Setting(settingPath = "elasticsearch/column-setting.json")
public class ColumnDocument {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 专栏标题 —— ik 分词，索引时最大化分词，搜索时智能分词 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /** 专栏描述 —— ik 分词 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /** 专栏封面URL */
    @Field(type = FieldType.Keyword)
    private String cover;

    /** 作者ID */
    @Field(type = FieldType.Long)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 作者昵称 */
    @Field(type = FieldType.Keyword)
    private String authorName;

    /** 作者头像 */
    @Field(type = FieldType.Keyword)
    private String authorAvatar;

    /** 文章数量 */
    @Field(type = FieldType.Integer)
    private Integer articlesCount;

    /** 订阅数量 */
    @Field(type = FieldType.Integer)
    private Integer subscriptionCount;

    /** 浏览量 */
    @Field(type = FieldType.Long)
    private Long views;

    /** 状态: 0-草稿, 1-已发布 */
    @Field(type = FieldType.Integer)
    private Integer status;

    /** 创建时间 */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;

    /** 标题补全建议器 —— 用于 Completion Suggester */
    @CompletionField(maxInputLength = 50)
    private Completion titleSuggest;
}
