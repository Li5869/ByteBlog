package com.personblog.api.AIAPI;

/**
 * AI 评论 API 接口
 * 用于跨模块调用 AI 评论生成服务
 *
 * @author LSH
 */
public interface AICommentApi {

    /**
     * 根据文章内容生成 AI 评论
     *
     * @param content 文章内容
     * @return 生成的评论内容
     */
    String commentContent(String content);
}
