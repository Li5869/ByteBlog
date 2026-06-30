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
public class LikeSaveDBMessage {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    private String targetType;

    private Boolean isLike;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    private String targetTitle;

    private String targetContent;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long relatedId;
}
