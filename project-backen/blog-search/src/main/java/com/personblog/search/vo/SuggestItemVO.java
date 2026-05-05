package com.personblog.search.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class SuggestItemVO {

    /** 雪花ID，序列化为字符串避免精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 展示文本：文章/问答标题 或 博主昵称 */
    private String title;

    /** 博主头像（仅 type=author 时有值） */
    private String avatar;

    /** 类型：article / question / author */
    private String type;
}