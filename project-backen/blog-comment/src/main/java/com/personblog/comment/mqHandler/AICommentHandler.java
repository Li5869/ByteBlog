package com.personblog.comment.mqHandler;

import com.personblog.api.AIAPI.AICommentApi;
import com.personblog.comment.dto.CommentCreateDTO;
import com.personblog.comment.service.ICommentService;
import com.personblog.common.dto.Comment.AICommentDTO;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.common.config.mqConfig.CommentMqConfig.AI_COMMENT_QUEUE;

@RequiredArgsConstructor
@Component
@Slf4j
public class AICommentHandler {
    private final ICommentService commentService;
    private final AICommentApi aiCommentApi;
    @RabbitListener(queues = AI_COMMENT_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void AiCommentSend(AICommentDTO dto, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            String s = aiCommentApi.commentContent(dto.getArticleContent());
            CommentCreateDTO createDTO = new CommentCreateDTO();
            createDTO.setUserId((long) -1);
            createDTO.setContent(s);
            createDTO.setArticleId(dto.getArticleId());
            createDTO.setArticleTitle(dto.getArticleTitle());
            commentService.createComment(createDTO);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("AI 评论处理失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
