package com.personblog.article.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.article.dto.ColumnArticleDTO;
import com.personblog.article.dto.ColumnCreateDTO;
import com.personblog.article.dto.ColumnUpdateDTO;
import com.personblog.article.entity.Column;
import com.personblog.article.vo.*;
import com.personblog.common.dto.Column.ColumnQueryDTO;

import java.util.List;

/**
 * 专栏服务接口
 *
 * @author LSH
 */
public interface IColumnService extends IService<Column> {

    /**
     * 创建专栏
     * @param userId 当前用户ID
     * @param dto 创建参数
     * @return 创建结果
     */
    ColumnCreateVO createColumn(Long userId, ColumnCreateDTO dto);

    /**
     * 更新专栏
     * @param userId 当前用户ID
     * @param columnId 专栏ID
     * @param dto 更新参数
     * @return 更新结果
     */
    ColumnCreateVO updateColumn(Long userId, Long columnId, ColumnUpdateDTO dto);

    /**
     * 删除专栏
     * @param userId 当前用户ID
     * @param columnId 专栏ID
     */
    void deleteColumn(Long userId, Long columnId);

    /**
     * 获取专栏详情
     * @param columnId 专栏ID
     * @param currentUserId 当前用户ID（可为null）
     * @return 专栏详情
     */
    ColumnDetailVO getColumnDetail(Long columnId, Long currentUserId);

    /**
     * 分页查询专栏列表
     * @param dto 查询参数
     * @return 分页结果
     */
    Page<ColumnListVO> getColumnPage(ColumnQueryDTO dto);

    /**
     * 获取我的专栏列表
     * @param userId 用户ID
     * @return 专栏列表
     */
    List<MyColumnVO> getMyColumns(Long userId);
    /**
     * 获取专栏标题
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
     * 添加文章到专栏（业务方法）
     * 包含登录校验、权限校验、文章数量上限校验等
     * @param userId 当前用户ID
     * @param columnId 专栏ID
     * @param dto 文章参数
     */
    void addArticles(Long userId, Long columnId, ColumnArticleDTO dto);

    /**
     * 从专栏移除文章（业务方法）
     * 包含登录校验、权限校验等
     * @param userId 当前用户ID
     * @param columnId 专栏ID
     * @param dto 文章参数
     */
    void removeArticles(Long userId, Long columnId, ColumnArticleDTO dto);

    /**
     * 获取可添加到专栏的文章（业务方法）
     * 返回用户已发布但不在该专栏中的文章
     * @param userId 用户ID
     * @param columnId 专栏ID
     * @return 文章列表
     */
    List<MyArticleVO> getAvailableArticles(Long userId, Long columnId);

    /**
     * 订阅专栏（业务方法）
     * 包含登录校验、专栏状态校验、自订阅校验、重复订阅校验等
     * @param userId 当前用户ID
     * @param columnId 专栏ID
     */
    void subscribeColumn(Long userId, Long columnId);

    /**
     * 取消订阅专栏（业务方法）
     * 包含登录校验
     * @param userId 当前用户ID
     * @param columnId 专栏ID
     */
    void unsubscribeColumn(Long userId, Long columnId);

    /**
     * 检查当前用户是否已订阅专栏（业务方法）
     * 未登录用户返回false
     * @param userId 用户ID
     * @param columnId 专栏ID
     * @return 是否已订阅
     */
    Boolean checkSubscribed(Long userId, Long columnId);

    /**
     * 获取用户订阅的专栏列表（业务方法）
     * @param userId 用户ID
     * @return 订阅列表
     */
    List<SubscriptionVO> getSubscriptions(Long userId);

    /**
     * 获取专栏的订阅用户列表（业务方法）
     * @param columnId 专栏ID
     * @return 订阅用户列表
     */
    List<SubscriberVO> getSubscribers(Long columnId);

    /**
     * 获取热门专栏列表
     * 根据订阅量排序，返回前N个已发布的专栏
     * @param limit 数量限制
     * @return 热门专栏列表
     */
    List<ColumnListVO> getHotColumns(int limit);
}
