package com.personblog.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.article.entity.ColumnSubscription;
import com.personblog.article.mapper.ColumnSubscriptionMapper;
import com.personblog.article.service.IColumnSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 专栏订阅服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnSubscriptionServiceImpl extends ServiceImpl<ColumnSubscriptionMapper, ColumnSubscription> implements IColumnSubscriptionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void subscribe(Long userId, Long columnId) {
        // 检查是否已订阅
        if (exists(userId, columnId)) {
            return;
        }

        // 创建订阅记录
        ColumnSubscription subscription = new ColumnSubscription();
        subscription.setUserId(userId);
        subscription.setColumnId(columnId);
        subscription.setCreatedAt(LocalDateTime.now());

        save(subscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unsubscribe(Long userId, Long columnId) {
        LambdaQueryWrapper<ColumnSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ColumnSubscription::getUserId, userId)
                .eq(ColumnSubscription::getColumnId, columnId);

        remove(wrapper);
    }

    @Override
    public boolean exists(Long userId, Long columnId) {
        return baseMapper.exists(userId, columnId);
    }

    @Override
    public List<Long> getColumnIdsByUserId(Long userId) {
        return lambdaQuery()
                .eq(ColumnSubscription::getUserId, userId)
                .select(ColumnSubscription::getColumnId)
                .list()
                .stream()
                .map(ColumnSubscription::getColumnId)
                .toList();
    }

    @Override
    public List<Long> getUserIdsByColumnId(Long columnId) {
        return lambdaQuery()
                .eq(ColumnSubscription::getColumnId, columnId)
                .select(ColumnSubscription::getUserId)
                .list()
                .stream()
                .map(ColumnSubscription::getUserId)
                .toList();
    }

    @Override
    public Long countByColumnId(Long columnId) {
        return lambdaQuery()
                .eq(ColumnSubscription::getColumnId, columnId)
                .count();
    }

    @Override
    public Long countByUserId(Long userId) {
       return lambdaQuery()
                .eq(ColumnSubscription::getUserId, userId)
                .count();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAllByColumnId(Long columnId) {
        LambdaQueryWrapper<ColumnSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ColumnSubscription::getColumnId, columnId);

        baseMapper.delete(wrapper);
    }
}
