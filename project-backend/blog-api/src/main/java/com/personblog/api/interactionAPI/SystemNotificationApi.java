package com.personblog.api.interactionAPI;

import com.personblog.common.dto.MqMessage.notifaction.NotificationMessage;

/**
 * 系统通知API接口
 * 用于跨模块调用系统通知服务
 *
 * @author LSH
 */
public interface SystemNotificationApi {

    /**
     * 保存系统通知
     *
     * @param dto 通知消息
     */
    void saveNotification(NotificationMessage dto);
}
