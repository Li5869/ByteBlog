package com.personblog.common.dto.Interaction;

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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long followingId;
    private Boolean isFollow;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long followerId;
}
