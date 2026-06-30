package com.personblog.api.messageAPI;

import com.personblog.common.dto.Notification.UnreadCountVO;

/**
 * 会话 API 接口
 * 用于跨模块调用私信会话服务
 *
 * @author LSH
 */
public interface ConversationApi {

    /**
     * 获取用户未读消息数
     *
     * @param userId 用户ID
     * @return 未读消息数信息
     */
    UnreadCountVO getUnreadCount(Long userId);
}
