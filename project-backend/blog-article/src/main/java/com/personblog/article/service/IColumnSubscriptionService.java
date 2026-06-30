package com.personblog.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.article.entity.ColumnSubscription;

import java.util.List;

/**
 * 专栏订阅服务接口
 *
 * @author LSH
 */
public interface IColumnSubscriptionService extends IService<ColumnSubscription> {

    /**
     * 订阅专栏
     *
     * @param userId   用户ID
     * @param columnId 专栏ID
     */
    void subscribe(Long userId, Long columnId);

    /**
     * 取消订阅专栏
     *
     * @param userId   用户ID
     * @param columnId 专栏ID
     */
    void unsubscribe(Long userId, Long columnId);

    /**
     * 检查用户是否已订阅专栏
     * @param userId 用户ID
     * @param columnId 专栏ID
     * @return 是否已订阅
     */
    boolean exists(Long userId, Long columnId);

    /**
     * 获取用户订阅的专栏ID列表
     * @param userId 用户ID
     * @return 专栏ID列表
     */
    List<Long> getColumnIdsByUserId(Long userId);

    /**
     * 获取专栏的订阅用户ID列表
     * @param columnId 专栏ID
     * @return 用户ID列表
     */
    List<Long> getUserIdsByColumnId(Long columnId);

    /**
     * 统计专栏的订阅数量
     * @param columnId 专栏ID
     * @return 订阅数量
     */
    Long countByColumnId(Long columnId);

    /**
     * 统计用户的订阅数量
     * @param userId 用户ID
     * @return 订阅数量
     */
    Long countByUserId(Long userId);

    /**
     * 删除专栏时，删除所有订阅记录
     *
     * @param columnId 专栏ID
     */
    void removeAllByColumnId(Long columnId);
}
