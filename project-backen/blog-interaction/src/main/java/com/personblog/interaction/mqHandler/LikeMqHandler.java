package com.personblog.interaction.mqHandler;

import com.personblog.api.articleAPI.ArticleInfoAPI;
import com.personblog.api.interactionAPI.CommentApi;
import com.personblog.api.questionAPI.AnswerApi;
import com.personblog.api.questionAPI.QuestionApi;
import com.personblog.common.dto.Interaction.LikeMessageDTO;
import com.personblog.interaction.bizService.BizLikeService;
import com.personblog.interaction.dto.MqMessage.LikeSaveDBMessageDTO;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.personblog.common.config.mqConfig.InteractionMqConfig.LIKE_DB_QUEUE;
import static com.personblog.common.config.mqConfig.InteractionMqConfig.LIKE_QUEUE;
import static com.personblog.common.constant.TargetTypeConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeMqHandler {
    private final ArticleInfoAPI articleInfoAPI;
    private final CommentApi commentApi;
    private final BizLikeService likeService;
    private final QuestionApi questionApi;
    private final AnswerApi answerApi;
    @RabbitListener(queues = LIKE_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handlerLikeMessage(List<LikeMessageDTO> dtos, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            //将dtos按照dto.targetType分组，map<String,List<LikeMessageDTO>>
            Map<String,List<LikeMessageDTO>> map = dtos.stream()
                    .collect(Collectors.groupingBy(LikeMessageDTO::getTargetType));
            //更新文章点赞数
            if(map.get(ARTICLE)!=null){
                articleInfoAPI.updateLikeCount(map.get(ARTICLE));
            }
            //更新评论点赞数
            if(map.get(COMMENT)!=null){
                commentApi.updateLikes(map.get(COMMENT));
            }
            //更新问题点赞数
            if(map.get(QUESTION)!=null){
                questionApi.updateLikeCount(map.get(QUESTION));
            }
            //更新回答点赞数
            if(map.get(ANSWER)!=null){
                answerApi.updateLikeCount(map.get(ANSWER));
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("点赞消息处理失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = LIKE_DB_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void SaveDate2DB(LikeSaveDBMessageDTO dto, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("开始存库点赞记录");
            likeService.save2DB(dto);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("点赞存库失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
