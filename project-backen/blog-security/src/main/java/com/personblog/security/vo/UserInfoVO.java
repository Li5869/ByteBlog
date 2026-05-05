package com.personblog.security.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户信息返回 VO
 * 
 * 用于返回用户信息给前端
 * 包含用户基本信息和登录后的 Token
 * 
 * @author LSH
 */
@Data
@Schema(description = "用户信息返回")
public class UserInfoVO {
    
    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long id;
    
    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "admin")
    private String username;
    
    /**
     * 昵称
     */
    @Schema(description = "昵称", example = "管理员")
    private String nickname;
    
    /**
     * 头像URL
     */
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;
    
    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;
    
    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    
    /**
     * 个人简介
     */
    @Schema(description = "个人简介", example = "这是一个个人简介")
    private String bio;
    
    /**
     * 是否为管理员
     */
    @Schema(description = "是否管理员", example = "false")
    private Boolean isAdmin;
    
    /**
     * 访问令牌
     * 
     * 登录成功后返回的 JWT Token
     * 前端需要在后续请求的 Authorization 头中携带此 Token
     * 格式：Bearer {token}
     */
    @Schema(description = "访问令牌（JWT Token）")
    private String token;

    /**
     * 刷新令牌
     *
     * 用于在 Access Token 过期后获取新的 Token
     * 有效期 7 天，一次性使用
     * 前端需妥善存储（推荐 localStorage）
     */
    @Schema(description = "刷新令牌（Refresh Token，7天有效）")
    private String refreshToken;
}
