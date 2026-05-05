package com.personblog.api.interactionAPI;

import com.personblog.common.dto.Notification.sse.NotificationMessageDTO;

/**
 * 用户业务通知API接口
 * 用于跨模块调用用户业务通知服务（点赞、评论、收藏、关注等）
 *
 * @author LSH
 */
public interface NotificationApi {

    /**
     * 保存用户业务通知
     *
     * @param dto 通知消息
     * @return 通知ID
     */
    Long saveNotification(NotificationMessageDTO dto);
}
