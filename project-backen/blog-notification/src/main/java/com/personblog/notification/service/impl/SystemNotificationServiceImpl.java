package com.personblog.notification.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.common.dto.Notification.BatchDeleteResultVO;
import com.personblog.common.dto.Notification.UnreadCountVO;
import com.personblog.common.dto.Notification.sse.NotificationMessageDTO;
import com.personblog.common.exception.BizException;
import com.personblog.notification.dto.SystemNotificationQueryDTO;
import com.personblog.notification.entity.SystemNotification;
import com.personblog.notification.mapper.SystemNotificationMapper;
import com.personblog.notification.service.ISystemNotificationService;
import com.personblog.notification.vo.SystemNotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.personblog.common.enums.BizCodeEnum.*;

/**
 * 系统通知服务实现
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
public class SystemNotificationServiceImpl extends ServiceImpl<SystemNotificationMapper, SystemNotification> implements ISystemNotificationService {

    private final SystemNotificationMapper systemNotificationMapper;

    @Override
    public void saveNotification(NotificationMessageDTO dto) {
        SystemNotification notification = new SystemNotification();
        notification.setUserId(dto.getUserId());
        notification.setTitle(buildNotificationTitle(dto.getActionType()));
        notification.setContent(dto.getContent());
        notification.setIsRead(false);
        notification.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());

        this.save(notification);
    }

    /**
     * 根据行为类型构建通知标题
     */
    private String buildNotificationTitle(String actionType) {
        return switch (actionType) {
            case "moderation_approved" -> "审核通过";
            case "moderation_rejected" -> "审核未通过";
            default -> "系统通知";
        };
    }

    @Override
    public Page<SystemNotificationVO> getSystemNotificationPage(Long userId, SystemNotificationQueryDTO dto) {
        int current = dto.getCurrent() == null ? 1 : dto.getCurrent();
        int size = dto.getSize() == null ? 10 : Math.min(dto.getSize(), 100);

        Page<SystemNotification> page = new Page<>(current, size);
        LambdaQueryWrapper<SystemNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemNotification::getUserId, userId)
                .orderByDesc(SystemNotification::getCreatedAt);

        Page<SystemNotification> notificationPage = this.page(page, wrapper);

        Page<SystemNotificationVO> voPage = new Page<>(current, size, notificationPage.getTotal());
        List<SystemNotificationVO> voList = BeanUtil.copyToList(notificationPage.getRecords(), SystemNotificationVO.class);
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void markAsRead(Long userId, Long id) {
        SystemNotification notification = this.getById(id);
        if (notification == null) {
            throw new BizException(NOT_FOUND_NOTIFICATION);
        }

        if (!notification.getUserId().equals(userId)) {
            throw new BizException(NO_POWER);
        }

        if (!notification.getIsRead()) {
            systemNotificationMapper.markAsRead(id, userId);
        }
    }

    @Override
    public void deleteNotification(Long userId, Long id) {
        SystemNotification notification = this.getById(id);
        if (notification == null) {
            throw new BizException(NOT_FOUND_NOTIFICATION);
        }

        if (!notification.getUserId().equals(userId)) {
            throw new BizException(NO_POWER);
        }

        this.removeById(id);
    }

    @Override
    public BatchDeleteResultVO batchDeleteNotifications(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(PARAMETER_ERROR);
        }

        if (ids.size() > 100) {
            throw new BizException(PARAMETER_ERROR);
        }

        LambdaUpdateWrapper<SystemNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(SystemNotification::getId, ids)
                .eq(SystemNotification::getUserId, userId);

        return new BatchDeleteResultVO(ids.size());
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        LambdaQueryWrapper<SystemNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemNotification::getUserId, userId)
                .eq(SystemNotification::getIsRead, false);

        long count = this.count(wrapper);
        return new UnreadCountVO((int) count);
    }
}
