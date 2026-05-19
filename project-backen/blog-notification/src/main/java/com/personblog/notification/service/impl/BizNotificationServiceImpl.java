package com.personblog.notification.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.interactionAPI.NotificationApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.Notification.BizNotificationQueryDTO;
import com.personblog.common.dto.Notification.sse.NotificationMessageDTO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.exception.BizException;
import com.personblog.notification.entity.BizNotification;
import com.personblog.notification.mapper.BizNotificationMapper;
import com.personblog.notification.service.IBizNotificationService;
import com.personblog.notification.vo.BatchDeleteResultVO;
import com.personblog.notification.vo.BizNotificationVO;
import com.personblog.notification.vo.SenderVO;
import com.personblog.notification.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.personblog.common.enums.BizCodeEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BizNotificationServiceImpl extends ServiceImpl<BizNotificationMapper, BizNotification> implements IBizNotificationService, NotificationApi {

    private final BizNotificationMapper bizNotificationMapper;
    private final UseApi useApi;

    @Override
    public Page<BizNotificationVO> getBizNotificationPage(Long userId, BizNotificationQueryDTO dto) {
        int current = dto.getCurrent() == null ? 1 : dto.getCurrent();
        int size = dto.getSize() == null ? 10 : Math.min(dto.getSize(), 100);

        Page<BizNotification> page = new Page<>(current, size);
        LambdaQueryWrapper<BizNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizNotification::getUserId, userId);

        if (dto.getActionType() != null && !dto.getActionType().isEmpty()) {
            wrapper.eq(BizNotification::getActionType, dto.getActionType());
        }

        if (dto.getTargetType() != null && !dto.getTargetType().isEmpty()) {
            wrapper.eq(BizNotification::getTargetType, dto.getTargetType());
        }

        wrapper.orderByDesc(BizNotification::getCreatedAt);

        Page<BizNotification> notificationPage = this.page(page, wrapper);

        List<BizNotification> notifications = notificationPage.getRecords();
        if (notifications.isEmpty()) {
            Page<BizNotificationVO> voPage = new Page<>(current, size, notificationPage.getTotal());
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        Set<Long> senderIds = notifications.stream()
                .map(BizNotification::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, UserDTO> userMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<UserDTO> users = useApi.getUserInfo(senderIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        Map<Long, UserDTO> finalUserMap = userMap;
        List<BizNotificationVO> voList = notifications.stream()
                .map(notification -> {
                    BizNotificationVO vo = BeanUtil.copyProperties(notification, BizNotificationVO.class);
                    UserDTO sender = finalUserMap.get(notification.getSenderId());
                    if (sender != null) {
                        vo.setSender(new SenderVO(sender.getId(), sender.getNickname(), sender.getAvatar()));
                    } else {
                        vo.setSender(new SenderVO(notification.getSenderId(), "用户", ""));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        Page<BizNotificationVO> voPage = new Page<>(current, size, notificationPage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void markAsRead(Long userId, Long id) {
        BizNotification notification = this.getById(id);
        if (notification == null) {
            throw new BizException(NOT_FOUND_NOTIFICATION);
        }

        if (!notification.getUserId().equals(userId)) {
            throw new BizException(NO_POWER);
        }

        if (!notification.getIsRead()) {
            bizNotificationMapper.markAsRead(id, userId);
        }
    }

    @Override
    public void deleteNotification(Long userId, Long id) {
        BizNotification notification = this.getById(id);
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

        LambdaUpdateWrapper<BizNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(BizNotification::getId, ids)
                .eq(BizNotification::getUserId, userId);

        this.remove(wrapper);

        return new BatchDeleteResultVO(ids.size());
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        LambdaQueryWrapper<BizNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizNotification::getUserId, userId)
                .eq(BizNotification::getIsRead, false);

        long count = this.count(wrapper);
        return new UnreadCountVO((int) count);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<BizNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BizNotification::getUserId, userId)
                .eq(BizNotification::getIsRead, false)
                .set(BizNotification::getIsRead, true);
        this.update(wrapper);
    }

    @Override
    public Long saveNotification(NotificationMessageDTO dto) {
        try {
            BizNotification notification = new BizNotification();
            notification.setUserId(dto.getUserId());
            notification.setActionType(dto.getActionType());
            notification.setTargetType(dto.getTargetType());
            notification.setTargetId(dto.getTargetId());
            notification.setSenderId(dto.getSenderId());
            notification.setTargetTitle(dto.getTargetTitle());
            notification.setContent(dto.getContent());
            notification.setRelatedId(dto.getRelatedId());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            this.save(notification);
            log.info("保存通知成功, userId={}, actionType={}, notificationId={}", dto.getUserId(), dto.getActionType(), notification.getId());
            return notification.getId();
        } catch (Exception e) {
            log.error("保存通知失败, dto={}", dto, e);
            return null;
        }
    }
}
