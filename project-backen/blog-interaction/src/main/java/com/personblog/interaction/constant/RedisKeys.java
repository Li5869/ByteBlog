package com.personblog.interaction.constant;

public class RedisKeys {

    /**
     * 用户关注关系前缀
     * 完整 Key 格式：user:follow:{userId}
     * 存储内容：用户关注的作者ID集合
     * 数据类型：Set
     */
    public static final String USER_FOLLOW = "user:follow:";

    /**
     * 点赞数统计 Key 前缀
     * 完整 Key 格式：likes:times:type:{targetType}
     * 存储内容：各业务的点赞数统计
     * 数据类型：Hash
     *           field: targetId
     *           value: 点赞数
     */
    public static final String LIKES_TIMES_KEY_PREFIX = "likes:times:type:";

    /**
     * 文章收藏数统计 Key 前缀
     * 完整 Key 格式：collections:times:articleId:{articleId}
     * 存储内容：文章的收藏数量
     * 数据类型：String（数字）
     */
    public static final String COLLECTION_TIMES_KEY_PREFIX = "collections:times:articleId:";

    /**
     * 浏览历史 Key 前缀
     * 完整 Key 格式：browse:history:{userId}
     * 存储内容：用户的浏览历史记录
     * 数据类型：ZSet
     *           score: 时间戳
     *           member: articleId
     */
    public static final String BROWSE_HISTORY_KEY_PREFIX = "browse:history:";

    /**
     * 浏览历史活跃用户集合
     * 完整 Key 格式：browse:active:users
     * 存储内容：有浏览历史的用户ID集合
     * 数据类型：Set
     * 用途：替代 KEYS 命令，O(1) 获取需要同步的用户列表
     */
    public static final String BROWSE_ACTIVE_USERS = "browse:active:users";

}
