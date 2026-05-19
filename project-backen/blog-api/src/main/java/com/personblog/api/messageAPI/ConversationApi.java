package com.personblog.api.messageAPI;

import com.personblog.common.dto.Notification.UnreadCountVO;

public interface ConversationApi {

    UnreadCountVO getUnreadCount(Long userId);

}
