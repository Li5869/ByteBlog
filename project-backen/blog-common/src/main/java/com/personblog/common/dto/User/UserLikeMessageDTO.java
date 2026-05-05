package com.personblog.common.dto.User;

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
public class UserLikeMessageDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;
    private Integer delta;
}
