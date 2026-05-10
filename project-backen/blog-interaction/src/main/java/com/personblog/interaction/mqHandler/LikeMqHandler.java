package com.personblog.interaction.mqHandler;

import com.personblog.api.articleAPI.ArticleInfoAPI;
import com.personblog.api.interactionAPI.CommentApi;
import com.personblog.api.questionAPI.AnswerApi;
import com.personblog.api.questionAPI.QuestionApi;
import com.personblog.common.dto.Interaction.LikeMessageDTO;
import com.personblog.interaction.bizService.BizLikeService;
import com.personblog.interaction.dto.MqMessage.LikeSaveDBMessageDTO;
import com.personblog.interaction.dto.MqMessage.SyncLikeCacheMessageDTO;
import com.personblog.interaction.service.AnswerLikeService;
import com.personblog.interaction.service.ArticleLikeService;
import com.personblog.interaction.service.CommentLikeService;
import com.personblog.interaction.service.QuestionLikeService;
import com.personblog.interaction.strategy.LikeStrategy;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.personblog.common.config.mqConfig.InteractionMqConfig.*;
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
    private final ArticleLikeService articleLikeService;
    private final CommentLikeService commentLikeService;
    private final QuestionLikeService questionLikeService;
    private final AnswerLikeService answerLikeService;
    private final RedissonClient redissonClient;

    private final Map<String, LikeStrategy> likeStrategyMap = new HashMap<>();

    // 点赞缓存同步锁前缀
    private static final String LIKE_SYNC_LOCK_PREFIX = "like_sync_cache:";

    @PostConstruct
    public void init() {
        likeStrategyMap.put(ARTICLE, articleLikeService);
        likeStrategyMap.put(COMMENT, commentLikeService);
        likeStrategyMap.put(QUESTION, questionLikeService);
        likeStrategyMap.put(ANSWER, answerLikeService);
    }
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

    @RabbitListener(queues = LIKE_SYNC_CACHE_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void syncLikeCache(SyncLikeCacheMessageDTO dto, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // 同类型点赞缓存同步使用分布式锁串行执行，防止并发
        RLock lock = redissonClient.getLock(LIKE_SYNC_LOCK_PREFIX + dto.getTargetType());
        try {
            boolean tryLock = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!tryLock) {
                log.warn("获取锁超时, targetType={}, 消息将重新入队", dto.getTargetType());
                channel.basicNack(deliveryTag, false, true);
                return;
            }
            log.info("开始同步点赞缓存, targetType={}", dto.getTargetType());
            LikeStrategy likeStrategy = likeStrategyMap.get(dto.getTargetType());
            if (likeStrategy != null) {
                likeStrategy.AllSync2Cache();
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("同步点赞缓存失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
