package com.personblog.interaction.mqHandler;

import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.MqMessage.user.UserLikeMessageDTO;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.USER_LIKE_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLikeMqHandler {
    private final UseApi useApi;

    @RabbitListener(queues = USER_LIKE_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handlerUserLikeMessage(List<UserLikeMessageDTO> dtos, Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("开始处理用户受赞数更新，消息数量: {}", dtos.size());
            useApi.batchUpdateLikesCount(dtos);
            log.info("用户受赞数更新完成");
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("用户受赞数更新失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
