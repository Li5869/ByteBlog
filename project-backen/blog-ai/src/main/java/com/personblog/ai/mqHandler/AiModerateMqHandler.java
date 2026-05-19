package com.personblog.ai.mqHandler;

import com.personblog.ai.BizService.ContentModerationService;
import com.personblog.ai.dto.ContentModerationDTO;
import com.personblog.common.dto.Comment.AICommentDTO;
import com.personblog.common.dto.Moderate.AiModerateMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.ai.config.mqConfig.AiMqConfig.AI_MODERATE_QUEUE;
import static com.personblog.common.constant.MqRoutingConstants.COMMENT_EXCHANGE;
import static com.personblog.common.constant.MqRoutingConstants.COMMENT_ROUTING_KEY;
import static com.personblog.common.constant.StatusConstant.APPROVED;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;

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
    private final RabbitTemplate rabbitTemplate;
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

            // 调用审核服务，获取审核结果
            String reviewStatus = contentModerationService.moderate(dto);
            // 文章审核通过后，自动触发AI评论
            if (ARTICLE.equals(message.getBizType()) && APPROVED.equals(reviewStatus)) {
                AICommentDTO commentMessage = AICommentDTO.builder()
                        .articleId(message.getBizId())
                        .articleContent(message.getContent())
                        .articleTitle(message.getTitle())
                        .build();
                rabbitTemplate.convertAndSend(COMMENT_EXCHANGE, COMMENT_ROUTING_KEY, commentMessage);
                log.info("文章审核通过，已发送AI评论消息, articleId={}", message.getBizId());
            }

            log.info("AI审核完成, bizType={}, bizId={}, status={}", message.getBizType(), message.getBizId(), reviewStatus);

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
