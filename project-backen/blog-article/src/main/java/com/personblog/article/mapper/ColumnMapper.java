package com.personblog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.article.dto.column.ColumnQueryDTO;
import com.personblog.article.entity.Column;
import com.personblog.article.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专栏Mapper接口
 *
 * @author LSH
 */
@Mapper
public interface ColumnMapper extends BaseMapper<Column> {

    /**
     * 分页查询专栏列表
     * @param page 分页参数
     * @param dto 查询参数
     * @return 分页结果
     */
    Page<ColumnListVO> selectColumnPage(Page<?> page, @Param("dto") ColumnQueryDTO dto);

    /**
     * 查询专栏文章列表
     * @param columnId 专栏ID
     * @return 文章列表
     */
    List<ColumnDetailVO.ColumnArticleVO> selectColumnArticles(@Param("columnId") Long columnId);

    /**
     * 查询可添加到专栏的文章
     * @param userId 用户ID
     * @param columnId 专栏ID
     * @return 文章列表
     */
    List<MyArticleVO> selectAvailableArticles(@Param("userId") Long userId, @Param("columnId") Long columnId);

    /**
     * 查询用户订阅的专栏列表
     * @param userId 用户ID
     * @return 订阅列表
     */
    List<SubscriptionVO> selectSubscriptions(@Param("userId") Long userId);

    /**
     * 查询专栏的订阅用户列表
     * @param columnId 专栏ID
     * @return 订阅用户列表
     */
    List<SubscriberVO> selectSubscribers(@Param("columnId") Long columnId);

    /**
     * 查询热门专栏列表
     * 根据订阅量排序
     * @param limit 数量限制
     * @return 热门专栏列表
     */
    List<ColumnListVO> selectHotColumns(@Param("limit") int limit);
}
