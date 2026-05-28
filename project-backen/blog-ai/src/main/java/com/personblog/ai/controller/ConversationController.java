package com.personblog.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.ai.dto.ConversationQueryDTO;
import com.personblog.ai.service.ChatService;
import com.personblog.ai.vo.ConversationDetailVO;
import com.personblog.ai.vo.ConversationListVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI会话", description = "AI智能问答会话相关接口，会话管理")
@Slf4j
@RestController
@RequestMapping("/ai/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatService chatService;
    @Operation(summary = "创建会话", description = "创建一个新的AI对话会话")
    @PostMapping
    public JsonData<Long> createConversation() {
        Long conversationId = chatService.createConversation();
        return JsonData.buildSuccess(conversationId);
    }

    @Operation(summary = "获取会话列表", description = "获取当前用户的所有AI对话会话，按创建时间倒序排列")
    @GetMapping
    public JsonData<Page<ConversationListVO>> getConversationList(ConversationQueryDTO dto) {
        Page<ConversationListVO> result = chatService.getConversationList(dto);
        return JsonData.buildSuccess(result);
    }

    @Operation(summary = "获取会话详情", description = "获取指定会话的详细信息，包括所有历史消息")
    @GetMapping("/{id}")
    public JsonData<ConversationDetailVO> getConversationDetail(
            @Parameter(description = "会话ID") @PathVariable Long id) {
        ConversationDetailVO detail = chatService.getConversationDetail(id);
        return JsonData.buildSuccess(detail);
    }
    @Operation(summary = "删除会话", description = "删除指定的AI对话会话（逻辑删除）")
    @DeleteMapping("/{id}")
    public JsonData<Void> deleteConversation(
            @Parameter(description = "会话ID") @PathVariable Long id) {
        chatService.deleteConversation(id);
        return JsonData.buildSuccess();
    }
}
