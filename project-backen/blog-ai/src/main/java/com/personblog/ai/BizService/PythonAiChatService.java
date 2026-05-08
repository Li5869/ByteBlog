package com.personblog.ai.BizService;

import com.personblog.ai.constants.ChatEventTypeEnum;
import com.personblog.ai.dto.AiConversationSendDTO;
import com.personblog.ai.dto.PythonChatRequest;
import com.personblog.ai.dto.PythonStreamEvent;
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

import static com.personblog.ai.constants.ChatEventTypeEnum.DATA;
import static com.personblog.ai.constants.ChatEventTypeEnum.PARAM;
import static com.personblog.ai.constants.LLMType.ASSISTANT;
import static com.personblog.ai.constants.LLMType.USER;
import static com.personblog.common.config.mqConfig.AiMqConfig.AI_EXCHANGE;
import static com.personblog.common.config.mqConfig.AiMqConfig.AI_TITLE_KEY;
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

        // 分别收集最终回答和思考过程
        StringBuilder answer = new StringBuilder();
        StringBuilder thinking = new StringBuilder();

        return pythonAiWebClient.post()
                .uri("/api/v1/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .filter(event -> event.data() != null)
                .map(this::parseEvent)
                .filter(Objects::nonNull)
                // 按事件类型分流收集
                .doOnNext(event -> {
                    String eventData = (String) event.getEventData();
                    if (eventData == null) {
                        return;
                    }
                    int eventType = event.getEventType();
                    if (eventType == DATA.getValue()) {
                        // chunk 文本 → 最终回答
                        answer.append(eventData);
                    } else if (eventType == PARAM.getValue()) {
                        // thinking 分析 + tool_call 描述 → 思考过程
                        thinking.append(eventData);
                    }
                })
                // 流完成后异步保存消息（回答 + 思考过程）
                .doOnComplete(() -> {
                    String answers = answer.toString();
                    String reasonings = thinking.toString();
                    if (StringUtils.hasText(answers) || StringUtils.hasText(reasonings)) {
                        CompletableFuture.runAsync(() -> {
                            try {
                                saveMessageAndUpdateCount(conversationId, answers, reasonings);
                            } catch (Exception e) {
                                log.error("保存消息失败，会话ID: {}", conversationId, e);
                            }
                        });
                    }
                })
                .doOnNext(event -> {
                    if (event.getEventType() == ChatEventTypeEnum.STOP.getValue() && event.getEventData() != null) {
                        log.info("Python AI 流式响应完成，会话ID: {}", conversationId);
                    }
                })
                .doOnError(e -> log.error("调用 Python AI 服务失败: {}", e.getMessage()))
                .onErrorResume(e -> Flux.just(ChatEventVO.builder()
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .eventData("服务异常: " + e.getMessage())
                        .build()));
    }

    private void saveMessageAndUpdateCount(Long conversationId, String answers, String reasonings) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole(ASSISTANT);
        aiMessage.setContent(answers);
        aiMessage.setThinking(reasonings);
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

            String type = (String) dataMap.get("type");
            String content = (String) dataMap.get("content");
            String conversationId = dataMap.get("conversation_id") != null
                    ? dataMap.get("conversation_id").toString()
                    : null;

            PythonStreamEvent streamEvent = new PythonStreamEvent();
            streamEvent.setType(type);
            streamEvent.setContent(content);
            streamEvent.setConversationId(conversationId);

            return convertEvent(streamEvent);
        } catch (Exception e) {
            log.warn("解析SSE事件失败: {} | 原始数据: {}", e.getMessage(), event.data());
            return null;
        }
    }

    private ChatEventVO convertEvent(PythonStreamEvent event) {
        if (event == null || event.getType() == null) {
            return null;
        }

        return switch (event.getType()) {
            // thinking → 模型思考分析内容（前端展示在"思考过程"面板），映射为 PARAM 事件
            case "thinking" -> ChatEventVO.builder()
                    .eventType(PARAM.getValue())
                    .eventData(event.getContent())
                    .build();
            // chunk → 回答文本片段（打字机效果），映射为 DATA 事件
            case "chunk" -> ChatEventVO.builder()
                    .eventType(DATA.getValue())
                    .eventData(event.getContent())
                    .build();
            // tool_call → ReAct 循环中的工具调用（前端展示在"思考过程"面板），映射为 PARAM 事件
            case "tool_call" -> ChatEventVO.builder()
                    .eventType(PARAM.getValue())
                    .eventData(event.getContent())
                    .build();
            case "tool_result" -> {
                log.debug("工具执行结果: {}", event.getContent());
                yield null;
            }
            case "done" -> ChatEventVO.STOP_EVENT;
            case "error" -> {
                log.error("Python AI 返回错误: {}", event.getContent());
                yield ChatEventVO.builder()
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .eventData("错误: " + event.getContent())
                        .build();
            }
            default -> ChatEventVO.builder()
                    .eventType(DATA.getValue())
                    .eventData(event.getContent() != null ? event.getContent() : "")
                    .build();
        };
    }
}
