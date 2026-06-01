package com.personblog.question.constant;

public class RedisKeys {
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
}
