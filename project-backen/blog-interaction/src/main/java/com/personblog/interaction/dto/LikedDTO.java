package com.personblog.interaction.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class LikedDTO {
    //目标ID
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;
    //目标类型
    private String targetType;
    //是否点赞
    private Boolean isLike;
    //作者id
    private Long authorId;
    //目标标题(如有)
    private String targetTitle;
    //目标内容(如有)
    private String targetContent;
    //关联的内容ID（当targetType为comment/answer时，用于跳转定位）
    @JsonSerialize(using = ToStringSerializer.class)
    private Long relatedId;
}
