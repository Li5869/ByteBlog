package com.personblog.comment.constant;

public class RedisKeys {
    /**
     * 评论分页缓存前缀
     * 完整 Key 格式：comment:page:{targetType}:{targetId}
     * 存储内容：评论分页数据
     * 数据类型：String（JSON）
     */
    public static final String COMMENT_PAGE = "comment:page:";
}
