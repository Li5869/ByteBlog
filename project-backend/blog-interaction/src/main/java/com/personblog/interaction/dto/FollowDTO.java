package com.personblog.interaction.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class FollowDTO {
    //关注者id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long followingId;
    //当前关注状态
    private Boolean isFollow;
}
