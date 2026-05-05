package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.interaction.entity.Collection;
import com.personblog.interaction.vo.MyCollectionVO;
import com.personblog.interaction.vo.UserCollectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 收藏表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface CollectionMapper extends BaseMapper<Collection> {

    List<MyCollectionVO> selectMyCollections(@Param("userId") Long userId,
                                             @Param("offset") Integer offset,
                                             @Param("size") Integer size);

    List<UserCollectionVO> selectUserCollections(@Param("userId") Long userId,
                                                 @Param("offset") Integer offset,
                                                 @Param("size") Integer size);

    long countByUserId(@Param("userId") Long userId);
}
