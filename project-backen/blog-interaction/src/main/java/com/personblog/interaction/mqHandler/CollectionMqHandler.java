package com.personblog.interaction.mqHandler;

import com.personblog.api.articleAPI.ArticleAPI;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.MqMessage.Interaction.CollectionMessage;
import com.personblog.interaction.service.CollectionService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.COLLECTION_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionMqHandler {
    private final ArticleAPI articleAPI;
    private final UseApi useApi;
    private final CollectionService collectionService;
    @RabbitListener(queues = COLLECTION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handlerCollectionMessage(CollectionMessage dto, Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("开始处理收藏数更新，文章ID: {}, 收藏数: {}, 用户ID: {}, 增量: {}", 
                    dto.getArticleId(), dto.getCollectionTimes(), dto.getUserId(), dto.getDelta());
            
            articleAPI.updateCollectionCount(dto);
            
            if (dto.getUserId() != null && dto.getDelta() != null && dto.getDelta() != 0) {
                useApi.updateCollectionsCount(dto.getUserId(), dto.getDelta());
                collectionService.save2DB(dto.getArticleId(),dto.getUserId(),dto.getIsCollection());
            }
            
            log.info("收藏数更新完成");
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("收藏数更新失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
