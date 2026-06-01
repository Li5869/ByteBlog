package com.personblog.push.constant;

public class RedisKeys {
    /**
     * 在线用户集合
     * 完整 Key 格式：online:users
     * 存储内容：所有在线用户ID
     * 数据类型：Set
     */
    public static final String ONLINE_USERS = "online:users";





    /**
     * 用户在线详情前缀
     * 完整 Key 格式：user:online:{userId}
     * 存储内容：JSON格式的登录信息
     * 数据类型：String（JSON）
     * 过期时间：300秒（5分钟）
     */
    public static final String USER_ONLINE_DETAIL = "user:online:";



    /**
     * 获取用户在线详情 Key
     *
     * @param userId 用户ID
     * @return 完整的 Redis Key
     */
    public static String getUserOnlineDetailKey(Long userId) {
        return USER_ONLINE_DETAIL + userId;
    }
}
