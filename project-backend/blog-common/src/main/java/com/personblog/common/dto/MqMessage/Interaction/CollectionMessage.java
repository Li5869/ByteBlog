package com.personblog.common.dto.MqMessage.Interaction;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionMessage {
    //文章id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long articleId;
    //收藏数
    private Long collectionTimes;
    //点赞者id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;
    //增量
    private Integer delta;
    //是否收藏
    private Boolean isCollection;
    //作者id
    private Long authorId;
}
