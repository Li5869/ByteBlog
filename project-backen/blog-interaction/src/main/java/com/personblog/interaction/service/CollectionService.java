package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.interaction.dto.CollectionDTO;
import com.personblog.interaction.entity.Collection;
import com.personblog.interaction.vo.CollectionVO;
import com.personblog.interaction.vo.MyCollectionVO;
import com.personblog.interaction.vo.UserCollectionVO;

/**
 * <p>
 * 收藏表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface CollectionService extends IService<Collection> {

    CollectionVO doCollection(CollectionDTO dto);

    void save2DB(Long articleId, Long userId,Boolean isCollection);

    Page<MyCollectionVO> getMyCollections(Long userId, Integer current, Integer size);

    Page<UserCollectionVO> getUserCollections(Long userId, Integer current, Integer size);
}
