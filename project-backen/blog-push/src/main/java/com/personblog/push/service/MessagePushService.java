package com.personblog.push.service;

import com.personblog.push.sse.SseEmitterManager;
import com.personblog.push.vo.PushMessageVO;
import com.personblog.push.websocket.WebSocketHandler;
import com.personblog.push.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessagePushService {
    private final WebSocketHandler webSocketHandler;
    private final SseEmitterManager sseEmitterManager;
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
     * 推送未读消息数更新（通过 SSE，纯单向推送无需走 WebSocket）
     */
    public void pushUnreadCountUpdate(Long receiverId, int unreadCount) {
        if (!onlineStateService.isOnline(receiverId)) {
            return;
        }
        sseEmitterManager.sendToUser(receiverId, Map.of(
                "userId", String.valueOf(receiverId),
                "delta", unreadCount), "unread_update");
    }
}
