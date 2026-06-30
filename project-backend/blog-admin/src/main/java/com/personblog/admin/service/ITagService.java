package com.personblog.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.admin.dto.AdminTagQueryDTO;
import com.personblog.admin.entity.Tag;
import com.personblog.admin.vo.AdminTagVO;
import com.personblog.api.adminAPI.vo.TagVO;

import java.util.List;

/**
 * 标签表 服务类
 *
 * @author LSH
 */
public interface ITagService extends IService<Tag> {

    List<TagVO> getTagList(Integer size);

    List<TagVO> getTagListByIds(List<Long> ids);

    void invalidateTagCache();

    Page<AdminTagVO> getAdminTagList(AdminTagQueryDTO dto);

    AdminTagVO createTag(Tag tag);

    void updateTagByAdmin(Tag tag);

    void deleteTagByAdmin(Long id);
}
