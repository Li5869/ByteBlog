package com.personblog.api.columnAPI;

/**
 * 专栏API接口 - 跨模块调用
 *
 * @author LSH
 */
public interface ColumnApi {
    /**
     * 从redis获取更新专栏浏览量
     */
    void updateColumnView();
}
