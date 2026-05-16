package com.personblog.push.service;

import com.personblog.push.vo.PushMessageVO;
import com.personblog.push.websocket.WebSocketHandler;
import com.personblog.push.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessagePushService {
    private final WebSocketHandler webSocketHandler;
    private final OnlineStateService onlineStateService;


    public void pushMessage(PushMessageVO message){
        if(!onlineStateService.isOnline(message.getReceiverId())){
            return;
        }
        WebSocketMessage webSocketMessage = WebSocketMessage.privateMessage(
                message.getSenderId(),
                message.getSenderName(),
                message.getSenderAvatar(),
                message.getContent(),
                message.getMessageId(),
                message.getCreatedAt());
        webSocketHandler.sendToUser(message.getReceiverId(),webSocketMessage);
    }

    /**
     * 推送未读消息数更新
     *
     * @param receiverId 接收者ID
     * @param unreadCount 未读消息数
     */
    public void pushUnreadCountUpdate(Long receiverId, int unreadCount) {
        if (!onlineStateService.isOnline(receiverId)) {
            return;
        }
        WebSocketMessage message = WebSocketMessage.unreadUpdate(receiverId, unreadCount);
        webSocketHandler.sendToUser(receiverId, message);
    }
}
