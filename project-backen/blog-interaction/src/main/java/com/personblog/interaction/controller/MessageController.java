package com.personblog.interaction.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.interaction.dto.BatchDeleteDTO;
import com.personblog.interaction.dto.MessageQueryDTO;
import com.personblog.interaction.dto.SendMessageDTO;
import com.personblog.interaction.service.IConversationService;
import com.personblog.interaction.service.MessageService;
import com.personblog.interaction.vo.BatchDeleteResultVO;
import com.personblog.interaction.vo.ConversationVO;
import com.personblog.interaction.vo.MessageVO;
import com.personblog.interaction.vo.UnreadCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "私信管理", description = "私信相关接口")
@RestController
@RequestMapping("/interaction/messages")
@RequiredArgsConstructor
public class MessageController {

    private final IConversationService conversationService;
    private final MessageService messageService;

    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表", description = "获取当前用户的私信会话列表")
    public JsonData<List<ConversationVO>> getConversationList() {
        Long userId = UserContextHolder.getUserId();
        List<ConversationVO> list = conversationService.getConversationList(userId);
        return JsonData.buildSuccess(list);
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "获取消息历史", description = "获取与指定用户的私信历史记录")
    public JsonData<Page<MessageVO>> getMessageHistory(
            @Parameter(description = "对方用户ID") @PathVariable Long userId,
            MessageQueryDTO dto) {
        Long currentUserId = UserContextHolder.getUserId();
        Page<MessageVO> page = messageService.getMessageHistory(currentUserId, userId, dto);
        return JsonData.buildSuccess(page);
    }

    @PutMapping("/read/{userId}")
    @Operation(summary = "标记消息已读", description = "将与指定用户的消息标记为已读")
    public JsonData<Void> markAsRead(
            @Parameter(description = "发送者用户ID") @PathVariable Long userId) {
        Long currentUserId = UserContextHolder.getUserId();
        messageService.markAsRead(currentUserId, userId);
        return JsonData.buildSuccess();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取私信未读数", description = "获取当前用户所有私信的未读消息总数")
    public JsonData<UnreadCountVO> getUnreadCount() {
        Long userId = UserContextHolder.getUserId();
        UnreadCountVO result = messageService.getUnreadCount(userId);
        return JsonData.buildSuccess(result);
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "删除会话", description = "删除指定的私信会话（不会删除消息记录）")
    public JsonData<Void> deleteConversation(
            @Parameter(description = "会话ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        conversationService.deleteConversation(userId, id);
        return JsonData.buildSuccess();
    }

    @DeleteMapping("/conversations/batch")
    @Operation(summary = "批量删除会话", description = "批量删除多个私信会话（不会删除消息记录）")
    public JsonData<BatchDeleteResultVO> batchDeleteConversations(
            @Valid @RequestBody BatchDeleteDTO dto) {
        Long userId = UserContextHolder.getUserId();
        BatchDeleteResultVO result = conversationService.batchDeleteConversations(userId, dto.getIds());
        return JsonData.buildSuccess(result);
    }
    @PostMapping
    @Operation(summary = "发送私信")
    public JsonData<MessageVO> sendMessage(
            @RequestBody @Valid SendMessageDTO dto) {

        Long senderId = UserContextHolder.getUserId();

        // 防止给自己发消息
        if (senderId.equals(dto.getReceiverId())) {
           throw new BizException(BizCodeEnum.CANT_SENT_TO_ME);
        }

        MessageVO message = messageService.sendMessage(senderId, dto.getReceiverId(), dto.getContent());
        return JsonData.buildSuccess(message);
    }

}
