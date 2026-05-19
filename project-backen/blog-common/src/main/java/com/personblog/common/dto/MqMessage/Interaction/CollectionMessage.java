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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long articleId;
    private Long collectionTimes;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private Integer delta;
    private Boolean isCollection;
}
