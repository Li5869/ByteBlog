package com.personblog.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.ai.BizService.PythonAiChatService;
import com.personblog.ai.dto.ChatMessageDTO;
import com.personblog.ai.dto.ConversationQueryDTO;
import com.personblog.ai.service.ChatService;
import com.personblog.ai.vo.ChatEventVO;
import com.personblog.ai.vo.ConversationDetailVO;
import com.personblog.ai.vo.ConversationListVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "AI聊天", description = "AI智能问答相关接口，包括会话管理和流式对话")
@Slf4j
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final PythonAiChatService pythonAiChatService;

    @Operation(summary = "创建会话", description = "创建一个新的AI对话会话")
    @PostMapping("/conversations")
    public JsonData<Long> createConversation() {
        Long conversationId = chatService.createConversation();
        return JsonData.buildSuccess(conversationId);
    }

    @Operation(summary = "获取会话列表", description = "获取当前用户的所有AI对话会话，按创建时间倒序排列")
    @GetMapping("/conversations")
    public JsonData<Page<ConversationListVO>> getConversationList(ConversationQueryDTO dto) {
        Page<ConversationListVO> result = chatService.getConversationList(dto);
        return JsonData.buildSuccess(result);
    }

    @Operation(summary = "获取会话详情", description = "获取指定会话的详细信息，包括所有历史消息")
    @GetMapping("/conversations/{id}")
    public JsonData<ConversationDetailVO> getConversationDetail(
            @Parameter(description = "会话ID") @PathVariable Long id) {
        ConversationDetailVO detail = chatService.getConversationDetail(id);
        return JsonData.buildSuccess(detail);
    }

    @Operation(summary = "删除会话", description = "删除指定的AI对话会话（逻辑删除）")
    @DeleteMapping("/conversations/{id}")
    public JsonData<Void> deleteConversation(
            @Parameter(description = "会话ID") @PathVariable Long id) {
        chatService.deleteConversation(id);
        return JsonData.buildSuccess();
    }

    @Operation(summary = "Agent智能对话（流式响应）", description = "通过Python Agent服务进行智能对话，AI自动选择工具获取信息，以SSE流式方式返回响应")
    @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> agentChatStream(@Valid @RequestBody ChatMessageDTO dto) {
        return pythonAiChatService.streamChat(dto.getConversationId(), dto.getContent(),dto.getIsDeepThinking());
    }
}
