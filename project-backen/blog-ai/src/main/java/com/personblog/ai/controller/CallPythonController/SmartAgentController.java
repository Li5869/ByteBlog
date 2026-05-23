package com.personblog.ai.controller.CallPythonController;

import com.personblog.ai.BizService.PythonAiChatService;
import com.personblog.ai.dto.ChatMessageDTO;
import com.personblog.ai.vo.ChatEventVO;
import com.personblog.common.monitor.BusinessMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "AI聊天", description = "AI智能问答相关接口，包括会话管理和流式对话")
@Slf4j
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class SmartAgentController {
    private final PythonAiChatService pythonAiChatService;
    private final BusinessMetrics businessMetrics;
    @Operation(summary = "Agent智能对话（流式响应）", description = "通过Python Agent服务进行智能对话，AI自动选择工具获取信息，以SSE流式方式返回响应")
    @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> agentChatStream(@Valid @RequestBody ChatMessageDTO dto) {
        businessMetrics.recordAiCall();
        return pythonAiChatService.streamChat(dto.getConversationId(), dto.getContent());
    }
}
