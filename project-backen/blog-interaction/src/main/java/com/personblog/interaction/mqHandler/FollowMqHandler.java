package com.personblog.interaction.mqHandler;

import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.Interaction.FollowMessageDTO;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.common.config.mqConfig.InteractionMqConfig.FOLLOW_QUEUE;

/**
 * 关注消息处理器
 * 异步处理粉丝数和关注数的更新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowMqHandler {
    
    private final UseApi userApi;
    
    @RabbitListener(queues = FOLLOW_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleFollowMessage(FollowMessageDTO dto, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Long followingId = dto.getFollowingId();
        Boolean isFollow = dto.getIsFollow();
        Long followerId = dto.getFollowerId();
        
        log.info("处理关注消息: followerId={}, followingId={}, isFollow={}", followerId, followingId, isFollow);
        
        try {
            if (isFollow) {
                // 取关：减少被关注者的粉丝数，减少关注者的关注数
                userApi.updateFanCount(followingId, -1);
                userApi.updateFollowingCount(followerId, -1);
                log.info("取关成功: 被关注者粉丝数-1, 关注者关注数-1");
            } else {
                // 关注：增加被关注者的粉丝数，增加关注者的关注数
                userApi.updateFanCount(followingId, 1);
                userApi.updateFollowingCount(followerId, 1);
                log.info("关注成功: 被关注者粉丝数+1, 关注者关注数+1");
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理关注消息失败，消息将转入死信队列: followerId={}, followingId={}, isFollow={}", 
                followerId, followingId, isFollow, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
