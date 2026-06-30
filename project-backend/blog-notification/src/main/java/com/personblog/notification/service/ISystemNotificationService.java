package com.personblog.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.api.interactionAPI.SystemNotificationApi;
import com.personblog.common.dto.Notification.BatchDeleteResultVO;
import com.personblog.common.dto.Notification.UnreadCountVO;
import com.personblog.notification.dto.SystemNotificationQueryDTO;
import com.personblog.notification.entity.SystemNotification;
import com.personblog.notification.vo.SystemNotificationVO;

import java.util.List;

/**
 * 系统通知服务接口
 *
 * @author LSH
 */
public interface ISystemNotificationService extends IService<SystemNotification>, SystemNotificationApi {

    Page<SystemNotificationVO> getSystemNotificationPage(Long userId, SystemNotificationQueryDTO dto);

    void markAsRead(Long userId, Long id);

    void deleteNotification(Long userId, Long id);

    BatchDeleteResultVO batchDeleteNotifications(Long userId, List<Long> ids);

    UnreadCountVO getUnreadCount(Long userId);
}
