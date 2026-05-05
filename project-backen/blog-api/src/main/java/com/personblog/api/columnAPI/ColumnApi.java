package com.personblog.api.columnAPI;

/**
 * 专栏API接口 - 跨模块调用
 *
 * @author LSH
 */
public interface ColumnApi {

    /**
     * 获取专栏基本信息
     * @param columnId 专栏ID
     * @return 专栏标题
     */
    String getColumnTitle(Long columnId);

    /**
     * 更新专栏文章数量
     * @param columnId 专栏ID
     */
    void updateColumnArticleCount(Long columnId);
    /**
     * 从redis获取更新专栏浏览量
     */
    void updateColumnView();
}
