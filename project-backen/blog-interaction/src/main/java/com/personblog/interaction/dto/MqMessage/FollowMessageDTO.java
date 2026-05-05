package com.personblog.interaction.dto.MqMessage;

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
public class FollowMessageDTO {
    //被关注者id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long followingId;
    //当前关注状态
    private Boolean isFollow;
    //关注着id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long followerId;
}
