package com.personblog.common.constant;

/**
 * Redis Key 常量类
 *
 * 统一管理 Redis 中使用的 Key 前缀
 * 避免硬编码，方便维护和修改
 *
 * Key 命名规范：业务:功能:标识
 * 例如：user:token:xxx 表示用户模块的 token 功能
 *
 * @author LSH
 */
public class RedisKeys {
    // =============================================
    // 二、用户信息模块
    // =============================================

    /**
     * 用户信息缓存前缀
     * 完整 Key 格式：user:info:{userId}
     * 存储内容：用户基本信息
     * 数据类型：String（JSON）
     */
    public static final String USER_INFO = "user:info:";

    /**
     * 作者信息缓存前缀
     * 完整 Key 格式：user:author:{authorId}
     * 存储内容：作者详细信息
     * 数据类型：String（JSON）
     */
    public static final String USER_AUTHOR = "user:author:";
    // =============================================
    // 六、互动模块（点赞、收藏、浏览）
    // =============================================
    /**
     * 点赞用户集合 Key 前缀
     * 完整 Key 格式：likes:set:{targetType}:{targetId}
     * 存储内容：给业务点赞的用户ID集合
     * 数据类型：Set
     * 参数：targetType - 业务类型（如 article、comment）
     *       targetId - 业务ID
     */
    public static String LIKE_BIZ_KEY_PREFIX(String targetType, Long targetId) {
        return "likes:set:" + targetType + ":" + targetId;
    }

    /**
     * 文章收藏用户集合 Key 前缀
     * 完整 Key 格式：collections:set:articleId:{articleId}
     * 存储内容：收藏该文章的用户ID集合
     * 数据类型：Set
     */
    public static final String COLLECTION_USER_KEY_PREFIX = "collections:set:articleId:";

    /**
     * 浏览计数 Key
     * 完整 Key 格式：browse:count
     * 存储内容：各文章的浏览量增量
     * 数据类型：Hash
     *           field: articleId
     *           value: 增量浏览数
     * 用途：定时任务批量同步到数据库
     */
    public static final String BROWSE_COUNT_KEY = "browse:count";

    // =============================================
    // 七、缓存 Pub/Sub 频道
    // =============================================

    /**
     * 缓存删除通知频道
     * 用于多级缓存（L1 Caffeine）的跨节点一致性
     * 发布者：MultiLevelCacheUtil.evict()
     * 订阅者：CacheEvictListener
     */
    public static final String CACHE_EVICT_CHANNEL = "cache:evict";

    // =============================================
    // 八、缓存分布式锁
    // =============================================

    /**
     * 缓存重建分布式锁前缀
     * 完整 Key 格式：lock:cache:{cacheKey}
     * 用途：逻辑过期方案异步重建时，防止多节点重复回源 DB
     * 实现：Redisson RLock（tryLock 不等待）
     */
    public static final String CACHE_LOCK_PREFIX = "lock:cache:";

}