package com.personblog.common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.common.dto.AdminTagQueryDTO;
import com.personblog.common.entity.Tag;
import com.personblog.common.vo.AdminTagVO;
import com.personblog.common.vo.TagVO;

import java.util.List;

/**
 * <p>
 * 标签表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface ITagService extends IService<Tag> {

    List<TagVO> getTagList(Integer size);

    /**
     * 根据ID批量获取标签
     *
     * @param ids 标签ID列表
     * @return 标签VO列表
     */
    List<TagVO> getTagListByIds(List<Long> ids);

    /**
     * 清理标签列表本地缓存
     * 标签使用次数变化后调用，确保下次查询获取最新数据
     */
    void invalidateTagCache();

    // ==================== 管理端接口 ====================

    /**
     * 管理端获取标签列表（分页）
     * @param dto 查询参数
     * @return 标签分页列表
     */
    Page<AdminTagVO> getAdminTagList(AdminTagQueryDTO dto);

    /**
     * 管理端创建标签
     * @param tag 标签信息
     * @return 创建结果
     */
    AdminTagVO createTag(Tag tag);

    /**
     * 管理端更新标签
     * @param tag 标签信息
     */
    void updateTagByAdmin(Tag tag);

    /**
     * 管理端删除标签
     * @param id 标签ID
     */
    void deleteTagByAdmin(Long id);
}
