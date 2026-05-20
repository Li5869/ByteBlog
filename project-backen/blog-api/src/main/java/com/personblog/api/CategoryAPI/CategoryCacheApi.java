package com.personblog.api.CategoryAPI;

/**
 * 分类缓存 API 接口
 * 用于跨模块操作分类缓存
 *
 * @author LSH
 */
public interface CategoryCacheApi {

    /**
     * 清除指定分类的缓存
     * 当分类信息更新时调用
     *
     * @param id 分类ID
     */
    void removeById(Long id);
}
