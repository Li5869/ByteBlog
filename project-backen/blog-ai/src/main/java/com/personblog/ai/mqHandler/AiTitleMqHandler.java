package com.personblog.ai.mqHandler;

import com.personblog.ai.dto.AiConversationSendDTO;
import com.personblog.ai.entity.AiConversation;
import com.personblog.ai.service.IAiConversationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

import static com.personblog.common.config.mqConfig.AiMqConfig.AI_TITLE_QUEUE;

@Slf4j
@RequiredArgsConstructor
@Component
public class AiTitleMqHandler {
    private final ChatClient chatClient;
    private final IAiConversationService conversationService;
    @RabbitListener(queues = AI_TITLE_QUEUE,containerFactory = "rabbitListenerContainerFactory")
    public void AiSetTitle(AiConversationSendDTO dto, Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            Long conversationId = dto.getConversationId();
            String title = Objects.requireNonNull(chatClient.prompt()
                            .system("为用户提出的问题生成对话标题，要求客观并且简洁明了，10字以内")
                            .user(dto.getUserPrompt())
                            .call()
                            .chatResponse())
                    .getResult()
                    .getOutput()
                    .getText();
           conversationService.lambdaUpdate()
                    .eq(AiConversation::getId, conversationId)
                    .set(AiConversation::getTitle, title)
                    .update();
            channel.basicAck(deliveryTag, false);
        }
        catch (Exception e) {
            log.error("AI 对话标题生成失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);  // 异常时转入死信队列
        }
    }
}