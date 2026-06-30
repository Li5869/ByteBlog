package com.personblog.security.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "活跃博主返回对象")
public class ActiveUserVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户昵称")
    private String name;

    @Schema(description = "用户头像URL")
    private String avatar;

    @Schema(description = "文章数量")
    private Long articles;

    @Schema(description = "粉丝数量")
    private Long followers;

    @Schema(description = "当前用户是否已关注")
    private Boolean isFollowing;
}
