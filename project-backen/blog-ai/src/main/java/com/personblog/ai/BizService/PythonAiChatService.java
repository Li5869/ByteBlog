package com.personblog.ai.BizService;

import cn.hutool.json.JSONUtil;
import com.personblog.ai.dto.AiConversationSendDTO;
import com.personblog.ai.dto.pythonRequest.PythonChatRequest;
import com.personblog.ai.entity.AiConversation;
import com.personblog.ai.entity.AiMessage;
import com.personblog.ai.service.IAiConversationService;
import com.personblog.ai.service.IAiMessageService;
import com.personblog.ai.vo.ChatEventVO;
import com.personblog.common.utils.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.personblog.ai.config.mqConfig.AiMqConfig.AI_TITLE_KEY;
import static com.personblog.ai.constants.LLMType.ASSISTANT;
import static com.personblog.ai.constants.LLMType.USER;
import static com.personblog.ai.constants.PythonAiApiConstants.Chat.STREAM;
import static com.personblog.ai.constants.PythonAiApiConstants.*;
import static com.personblog.common.constant.MqRoutingConstants.AI_EXCHANGE;
import static com.personblog.common.constant.RedisKeys.REDIS_MEMORY_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonAiChatService {
    @Resource(name = "AiMessageExecutor")
    private Executor executor;
    private final WebClient pythonAiWebClient;
    private final IAiMessageService aiMessageService;
    private final IAiConversationService aiConversationService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(rollbackFor = Exception.class)
    public Flux<ChatEventVO> streamChat(Long conversationId, String message) {
        // 从当前请求上下文中获取用户ID（可能为 null，表示未登录）
        Long currentUserId = UserContextHolder.getUserId();
        String userIdStr = currentUserId != null ? currentUserId.toString() : null;
        log.debug("当前对话用户ID: {}", currentUserId);

        PythonChatRequest request = PythonChatRequest.builder()
                .conversationId(conversationId.toString())
                .message(message)
                .userId(userIdStr)
                .build();

        String key = REDIS_MEMORY_PREFIX + conversationId;
        if (!redisTemplate.hasKey(key)) {
            AiConversationSendDTO messageDto = AiConversationSendDTO.builder()
                    .conversationId(conversationId)
                    .userPrompt(message)
                    .build();
            rabbitTemplate.convertAndSend(AI_EXCHANGE, AI_TITLE_KEY, messageDto);
        }

        executor.execute(() -> saveHumanMessage(message, conversationId));

        log.info("调用 Python AI 服务，会话ID: {}", conversationId);

        StringBuilder answer = new StringBuilder();
        StringBuilder thinking = new StringBuilder();
        String[] toolCallsJsonHolder = {null};
        return pythonAiWebClient.post()
                .uri(STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .filter(event -> event.data() != null)
                .map(this::parseEvent)
                .filter(Objects::nonNull)
                // 按事件类型分流收集
                .doOnNext(event -> {
                    String eventData = (String) event.getData();
                    if (eventData == null) {
                        return;
                    }
                    String type = event.getType();
                    if (SseEvent.CHUNK.equals(type)) {
                        answer.append(eventData);
                    } else if (SseEvent.THINKING.equals(type) || SseEvent.TOOL_CALL.equals(type)) {
                        thinking.append(eventData);
                    }
                })
                // 流完成后异步保存消息（回答 + 思考过程 + 工具调用）
                .doOnComplete(() -> {
                    String answers = answer.toString();
                    String reasonings = thinking.toString();
                    if (StringUtils.hasText(answers) || StringUtils.hasText(reasonings)) {
                        CompletableFuture.runAsync(() -> {
                            try {
                                saveMessageAndUpdateCount(conversationId, answers, reasonings, toolCallsJsonHolder[0]);
                            } catch (Exception e) {
                                log.error("保存消息失败，会话ID: {}", conversationId, e);
                            }
                        });
                    }
                })
                .doOnNext(event -> {
                    if (SseEvent.DONE.equals(event.getType())) {
                        log.info("Python AI 流式响应完成，会话ID: {}", conversationId);
                        toolCallsJsonHolder[0] = (String) event.getData();
                    }
                })
                .doOnError(e -> log.error("调用 Python AI 服务失败: {}", e.getMessage()))
                .onErrorResume(e -> Flux.just(ChatEventVO.builder()
                        .type(SseEvent.ERROR)
                        .data(Msg.SERVICE_ERROR + e.getMessage())
                        .build()));
    }

    private void saveMessageAndUpdateCount(Long conversationId, String answers, String reasonings, String toolCallsJson) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole(ASSISTANT);
        aiMessage.setContent(answers);
        aiMessage.setThinking(reasonings);
        aiMessage.setToolCalls(toolCallsJson);
        aiConversationService.lambdaUpdate()
                .set(AiConversation::getLastMessage, answers)
                .setSql("message_count = message_count + " + 1)
                .eq(AiConversation::getId, conversationId)
                .update();
        aiMessageService.save(aiMessage);
    }

    private void saveHumanMessage(String message, Long conversationId) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setContent(message);
        aiMessage.setRole(USER);
        aiConversationService.lambdaUpdate()
                .set(AiConversation::getLastMessage, message)
                .setSql("message_count = message_count + " + 1)
                .eq(AiConversation::getId, conversationId)
                .update();
        aiMessageService.save(aiMessage);
    }

    @SuppressWarnings("unchecked")
    private ChatEventVO parseEvent(ServerSentEvent<?> event) {
        try {
            Object data = event.data();
            if (data == null) {
                return null;
            }

            Map<String, Object> dataMap;
            if (data instanceof Map) {
                dataMap = (Map<String, Object>) data;
            } else {
                return null;
            }

            String type = (String) dataMap.get(Fields.TYPE);
            String content = (String) dataMap.get(Fields.CONTENT);

            if (SseEvent.ERROR.equals(type)) {
                log.error("Python AI 返回错误: {}", content);
                return ChatEventVO.builder()
                        .type(SseEvent.ERROR)
                        .data(Msg.ERROR_PREFIX + content)
                        .build();
            }

            if (SseEvent.DONE.equals(type)) {
                Object toolCallsObj = dataMap.get("tool_calls");
                if (toolCallsObj != null) {
                    return ChatEventVO.builder()
                            .type(SseEvent.DONE)
                            .data(JSONUtil.toJsonStr(toolCallsObj))
                            .build();
                }
                return ChatEventVO.DONE_EVENT;
            }

            return ChatEventVO.builder()
                    .type(type)
                    .data(content)
                    .build();
        } catch (Exception e) {
            log.warn("解析SSE事件失败: {} | 原始数据: {}", e.getMessage(), event.data());
            return null;
        }
    }
}
