package com.personblog.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求 DTO
 * 
 * 用于接收前端提交的 Refresh Token
 * 在 Access Token 过期后，通过此接口获取新的 Token
 * 
 * @author LSH
 */
@Data
@Schema(description = "刷新令牌请求参数")
public class RefreshTokenDTO {

    /**
     * Refresh Token
     * 由登录接口或上一次刷新接口返回
     * 一次性使用，刷新后会签发新的 Refresh Token
     */
    @NotBlank(message = "Refresh Token不能为空")
    @Schema(description = "Refresh Token", requiredMode = Schema.RequiredMode.REQUIRED, example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
