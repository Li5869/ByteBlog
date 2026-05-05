package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.api.interactionAPI.SystemNotificationApi;
import com.personblog.interaction.dto.SystemNotificationQueryDTO;
import com.personblog.interaction.entity.SystemNotification;
import com.personblog.interaction.vo.BatchDeleteResultVO;
import com.personblog.interaction.vo.SystemNotificationVO;
import com.personblog.interaction.vo.UnreadCountVO;

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
