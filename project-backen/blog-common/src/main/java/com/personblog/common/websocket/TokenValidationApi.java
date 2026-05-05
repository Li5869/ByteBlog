package com.personblog.common.websocket;

/**
 * Token 验证接口
 * 用于 WebSocket 握手时的 Token 验证
 * 由 blog-security 模块实现
 *
 * @author LSH
 */
public interface TokenValidationApi {

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true 表示有效
     */
    boolean validateToken(String token);

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID，Token 无效时返回 null
     */
    Long getUserIdFromToken(String token);
}
