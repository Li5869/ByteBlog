package com.personblog.article.constant;

public class RedisKeys {
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

    /**
     * 全部分类缓存
     * 完整 Key 格式：category:all
     * 存储内容：所有分类列表
     * 数据类型：String（JSON）
     */
    public static final String CATEGORY_ALL = "category:all";

}
