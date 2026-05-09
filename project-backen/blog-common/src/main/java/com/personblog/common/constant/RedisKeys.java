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

    private RedisKeys() {
    }

    // =============================================
    // 一、用户认证模块
    // =============================================

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
     * 验证码前缀
     * 完整 Key 格式：captcha:{uuid}
     * 存储内容：验证码答案
     * 数据类型：String
     * 过期时间：5 分钟
     */
    public static final String CAPTCHA_CODE = "captcha:";

    /**
     * 登录失败次数前缀
     * 完整 Key 格式：login:fail:{username}
     * 存储内容：失败次数
     * 数据类型：String（数字）
     * 过期时间：30 分钟
     */
    public static final String LOGIN_FAIL_COUNT = "login:fail:";

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
     * 作者信息缓存前缀
     * 完整 Key 格式：user:author:{authorId}
     * 存储内容：作者详细信息
     * 数据类型：String（JSON）
     */
    public static final String USER_AUTHOR = "user:author:";

    /**
     * 活跃用户列表
     * 完整 Key 格式：user:active
     * 存储内容：活跃用户列表
     * 数据类型：List/ZSet
     */
    public static final String USER_ACTIVE = "user:active";

    /**
     * 用户关注关系前缀
     * 完整 Key 格式：user:follow:{userId}
     * 存储内容：用户关注的作者ID集合
     * 数据类型：Set
     */
    public static final String USER_FOLLOW = "user:follow:";

    // =============================================
    // 三、文章缓存模块
    // =============================================

    /**
     * 文章详情缓存前缀（已废弃，拆分为元数据+内容）
     * 完整 Key 格式：article:detail:{articleId}
     * 存储内容：文章详情信息（含正文，易造成大体积缓存）
     * 数据类型：String（JSON）
     */
    public static final String ARTICLE_DETAIL = "article:detail:";

    /**
     * 文章元数据缓存前缀
     * 完整 Key 格式：article:metadata:{articleId}
     * 存储内容：文章元数据（不含正文，体积小、加载快）
     * 数据类型：String（JSON）
     */
    public static final String ARTICLE_METADATA = "article:metadata:";

    /**
     * 文章正文内容缓存前缀
     * 完整 Key 格式：article:content:{articleId}
     * 存储内容：文章 Markdown 正文内容
     * 数据类型：String（JSON）
     */
    public static final String ARTICLE_CONTENT = "article:content:";

    /**
     * 文章分页列表缓存前缀
     * 完整 Key 格式：article:page:{queryHash}
     * 存储内容：文章分页数据
     * 数据类型：String（JSON）
     */
    public static final String ARTICLE_PAGE = "article:page:";

    /**
     * Banner 轮播图缓存
     * 完整 Key 格式：article:banners
     * 存储内容：轮播图文章列表
     * 数据类型：String（JSON）
     */
    public static final String ARTICLE_BANNERS = "article:banners";

    /**
     * 热门文章缓存前缀
     * 完整 Key 格式：article:hot:{period}
     * 存储内容：热门文章列表
     * 数据类型：String（JSON）
     */
    public static final String ARTICLE_HOT = "article:hot:";

    // =============================================
    // 四、专栏模块
    // =============================================

    /**
     * 专栏阅读量缓存 Key
     * 完整 Key 格式：column:read:count
     * 存储内容：各专栏的阅读量统计
     * 数据类型：Hash
     *           field: columnId
     *           value: 查看量
     * 用途：定时任务批量同步到数据库
     */
    public static final String COLUMN_READ_COUNT = "column:view:count";

    // =============================================
    // 五、问答模块
    // =============================================

    /**
     * 问题详情缓存前缀
     * 完整 Key 格式：question:detail:{questionId}
     * 存储内容：问题详情信息
     * 数据类型：String（JSON）
     */
    public static final String QUESTION_DETAIL = "question:detail:";

    /**
     * 问题分页列表缓存前缀
     * 完整 Key 格式：question:page:{queryHash}
     * 存储内容：问题分页数据
     * 数据类型：String（JSON）
     */
    public static final String QUESTION_PAGE = "question:page:";

    /**
     * 热门问题缓存
     * 完整 Key 格式：question:hot
     * 存储内容：热门问题列表
     * 数据类型：String（JSON）
     */
    public static final String QUESTION_HOT = "question:hot";

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

    /**
     * 浏览历史 Key 前缀
     * 完整 Key 格式：browse:history:{userId}
     * 存储内容：用户的浏览历史记录
     * 数据类型：ZSet
     *           score: 时间戳
     *           member: articleId
     */
    public static final String BROWSE_HISTORY_KEY_PREFIX = "browse:history:";

    // =============================================
    // 七、分类标签模块
    // =============================================

    /**
     * 全部分类缓存
     * 完整 Key 格式：category:all
     * 存储内容：所有分类列表
     * 数据类型：String（JSON）
     */
    public static final String CATEGORY_ALL = "category:all";

    /**
     * 标签列表缓存前缀
     * 完整 Key 格式：tag:list:{categoryId}
     * 存储内容：指定分类下的标签列表
     * 数据类型：String（JSON）
     */
    public static final String TAG_LIST = "tag:list:";

    // =============================================
    // 八、评论模块
    // =============================================

    /**
     * 评论分页缓存前缀
     * 完整 Key 格式：comment:page:{targetType}:{targetId}
     * 存储内容：评论分页数据
     * 数据类型：String（JSON）
     */
    public static final String COMMENT_PAGE = "comment:page:";

    // =============================================
    // 九、AI 服务模块
    // =============================================

    /**
     * AI 停止对话标志 Key 前缀
     * 完整 Key 格式：ai:chat:stop:{sessionId}
     * 存储内容：停止标志（用于中断 AI 对话）
     * 数据类型：String
     */
    public static final String REDIS_KEY_PREFIX = "ai:chat:stop:";

    /**
     * AI 对话记忆 Key 前缀
     * 完整 Key 格式：chat:memory:{sessionId}
     * 存储内容：AI 对话的上下文记忆
     * 数据类型：List/String
     */
    public static final String REDIS_MEMORY_PREFIX = "chat:memory:";

    /**
     * AI 对话默认 Key 前缀
     * 完整 Key 格式：CHAT:{sessionId}
     * 存储内容：Redis 对话记忆存储
     * 数据类型：String（JSON）
     */
    public static String DEFAULT_PREFIX = "CHAT:";

    // =============================================
    // 辅助方法
    // =============================================

    /**
     * 获取用户 Access Token 的完整 Key
     * 
     * @param token JWT Token
     * @return 完整的 Redis Key
     */
    public static String getUserTokenKey(String token) {
        return USER_TOKEN + token;
    }

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

    /**
     * 获取用户信息缓存的完整 Key
     * 
     * @param userId 用户ID
     * @return 完整的 Redis Key
     */
    public static String getUserInfoKey(Long userId) {
        return USER_INFO + userId;
    }

    /**
     * 获取验证码的完整 Key
     * 
     * @param uuid 验证码唯一标识
     * @return 完整的 Redis Key
     */
    public static String getCaptchaKey(String uuid) {
        return CAPTCHA_CODE + uuid;
    }

    /**
     * 获取登录失败次数的完整 Key
     * 
     * @param username 用户名
     * @return 完整的 Redis Key
     */
    public static String getLoginFailKey(String username) {
        return LOGIN_FAIL_COUNT + username;
    }

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
