package com.personblog.security.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 刷新令牌响应 VO
 * 
 * 用于返回新的 Access Token 和 Refresh Token
 * 前端收到后需更新本地存储的 Token 值
 * 
 * @author LSH
 */
@Data
@Schema(description = "刷新令牌返回结果")
public class RefreshTokenVO {

    /**
     * 新的 Access Token
     * 有效期 30 分钟
     * 前端需替换原有的 Access Token
     */
    @Schema(description = "新的Access Token（30分钟有效）")
    private String accessToken;

    /**
     * 新的 Refresh Token
     * 有效期 7 天
     * 前端需替换原有的 Refresh Token
     * 一次性使用，下次刷新时此 Token 将失效
     */
    @Schema(description = "新的Refresh Token（7天有效，一次性使用）")
    private String refreshToken;
}
