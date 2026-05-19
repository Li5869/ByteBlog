package com.personblog.api.adminAPI;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 标签 API 接口
 *
 * @author LSH
 */
public interface TagApi {

    /**
     * 获取热门标签列表
     *
     * @param limit 数量限制
     * @return 标签列表
     */
    List<TagVO> getHotTags(int limit);

    /**
     * 根据ID批量获取标签
     *
     * @param ids 标签ID列表
     * @return 标签列表
     */
    List<TagVO> getTagsByIds(Collection<Long> ids);

    /**
     * 根据名称批量获取标签DTO
     *
     * @param names 标签名称列表
     * @return 标签DTO列表
     */
    List<TagDTO> getTagsByNames(Set<String> names);

    /**
     * 检查标签ID是否存在
     *
     * @param tagIds 标签ID集合
     * @return 存在的标签数量
     */
    long countExistingTags(Set<Long> tagIds);

    /**
     * 批量创建标签
     *
     * @param tags 标签DTO列表
     */
    void saveTags(List<TagDTO> tags);

    /**
     * 更新标签使用次数
     *
     * @param tagId 标签ID
     * @param delta 变化量（正数增加，负数减少）
     */
    void updateTagUseCount(Long tagId, int delta);

    /**
     * 批量更新标签使用次数
     *
     * @param tagIds 标签ID集合
     * @param delta  变化量（正数增加，负数减少）
     */
    void batchUpdateTagUseCount(Set<Long> tagIds, int delta);

    /**
     * 清除标签缓存
     */
    void invalidateTagCache();
}
