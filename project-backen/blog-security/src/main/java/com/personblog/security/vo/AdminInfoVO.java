package com.personblog.security.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理员登录返回 VO
 *
 * @author LSH
 */
@Data
@Schema(description = "管理员登录信息")
public class AdminInfoVO {

    @Schema(description = "管理员ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "访问令牌（JWT Token）")
    private String token;

    @Schema(description = "刷新令牌（Refresh Token，7天有效）")
    private String refreshToken;
}
