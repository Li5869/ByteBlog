package com.personblog.ai.mqHandler;

import com.personblog.ai.BizService.ContentModerationService;
import com.personblog.ai.dto.ContentModerationDTO;
import com.personblog.common.dto.Moderate.AiModerateMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.common.config.mqConfig.AiMqConfig.AI_MODERATE_QUEUE;

/**
 * AI内容审核消息处理器
 * 异步处理文章、评论、问题等内容的AI审核
 * 使用手动ACK模式，审核失败时消息转入死信队列
 *
 * @author LSH
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AiModerateMqHandler {

    private final ContentModerationService contentModerationService;

    /**
     * 处理内容审核消息
     * 接收来自各业务模块的审核请求，调用AI进行内容审核
     * 审核成功：手动ACK确认消息
     * 审核失败：手动NACK拒绝消息，消息转入死信队列
     *
     * @param message     审核消息，包含业务ID、业务类型、待审核内容
     * @param channel     RabbitMQ通道
     * @param deliveryTag 消息投递标签
     */
    @RabbitListener(queues = AI_MODERATE_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleModerateMessage(AiModerateMessage message,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到AI审核消息, bizType={}, bizId={}", message.getBizType(), message.getBizId());

        try {
            // 构建审核请求DTO
            ContentModerationDTO dto = new ContentModerationDTO();
            dto.setContent(message.getContent());
            dto.setContentType(message.getBizType());
            dto.setBizId(message.getBizId());
            dto.setAuthorId(message.getAuthorId());
            dto.setTitle(message.getTitle());

            // 调用审核服务
            contentModerationService.moderate(dto);

            log.info("AI审核完成, bizType={}, bizId={}", message.getBizType(), message.getBizId());

            // 审核成功，手动ACK确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            // 审核失败，记录日志并拒绝消息，消息转入死信队列
            log.error("AI审核失败, bizType={}, bizId={}, error={}",
                    message.getBizType(), message.getBizId(), e.getMessage(), e);

            // 手动NACK拒绝消息，requeue=false表示不重新入队，转入死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
