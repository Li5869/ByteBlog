package com.personblog.api.articleAPI;

public interface HotArticleAPI {
    /**
     * 刷新热门文章标记
     * 根据综合热度分（浏览量+点赞+收藏+评论）计算 Top N 并更新 is_hot 字段
     */
    void refreshHotArticles();
}
