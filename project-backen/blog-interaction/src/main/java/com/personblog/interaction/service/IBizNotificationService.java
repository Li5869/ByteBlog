package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.common.dto.Notification.BizNotificationQueryDTO;
import com.personblog.interaction.entity.BizNotification;
import com.personblog.interaction.vo.BatchDeleteResultVO;
import com.personblog.interaction.vo.BizNotificationVO;
import com.personblog.interaction.vo.UnreadCountVO;

import java.util.List;

public interface IBizNotificationService extends IService<BizNotification> {

    Page<BizNotificationVO> getBizNotificationPage(Long userId, BizNotificationQueryDTO dto);

    void markAsRead(Long userId, Long id);

    void deleteNotification(Long userId, Long id);

    BatchDeleteResultVO batchDeleteNotifications(Long userId, List<Long> ids);

    UnreadCountVO getUnreadCount(Long userId);

    void markAllAsRead(Long userId);

}
