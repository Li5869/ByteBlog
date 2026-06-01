package com.personblog.security.constant;

public class RedisKeys {
    /**
     * 用户 Access Token 前缀
     * 完整 Key 格式：user:token:{token}
     * 存储内容：LoginUser 对象（用户登录信息）
     * 数据类型：String
     * 过期时间：30 分钟
     */
    public static final String USER_TOKEN = "user:token:";


    /**
     * Refresh Token 前缀
     * 完整 Key 格式：user:refresh_token:{userId}
     * 存储内容：Refresh Token 字符串
     * 数据类型：String
     * 过期时间：7 天（与配置一致）
     * 用途：用于签发新的 Access Token，一次性使用（Rotation 机制）
     */
    public static final String USER_REFRESH_TOKEN = "user:refresh_token:";

    /**
     * 用户当前登录 Token 前缀
     * 完整 Key 格式：user:login:{userId}
     * 存储内容：当前用户的 Access Token
     * 数据类型：String
     * 过期时间：30 分钟（与 Access Token 一致）
     * 用途：实现单点登录踢人机制，一个账号只能在一个地方登录
     */
    public static final String USER_LOGIN_TOKEN = "user:login:";


    /**
     * 获取用户 Refresh Token 的完整 Key
     *
     * @param userId 用户ID
     * @return 完整的 Redis Key
     */
    public static String getUserRefreshTokenKey(Long userId) {
        return USER_REFRESH_TOKEN + userId;
    }

    /**
     * 获取用户当前登录 Token 的完整 Key
     *
     * @param userId 用户ID
     * @return 完整的 Redis Key
     */
    public static String getUserLoginTokenKey(Long userId) {
        return USER_LOGIN_TOKEN + userId;
    }
}
