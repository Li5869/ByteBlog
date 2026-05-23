package com.personblog.interaction.bizService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.interactionAPI.LikeApi;
import com.personblog.common.dto.MqMessage.Interaction.LikeMessage;
import com.personblog.common.dto.MqMessage.Interaction.LikeSaveDBMessage;
import com.personblog.common.dto.MqMessage.Interaction.SyncLikeCacheMessage;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.interaction.dto.LikedDTO;
import com.personblog.interaction.mapper.ArticleLikeMapper;
import com.personblog.interaction.service.AnswerLikeService;
import com.personblog.interaction.service.ArticleLikeService;
import com.personblog.interaction.service.CommentLikeService;
import com.personblog.interaction.service.QuestionLikeService;
import com.personblog.interaction.strategy.LikeStrategy;
import com.personblog.interaction.vo.LikedVO;
import com.personblog.interaction.vo.MyLikeVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.personblog.common.constant.RedisKeys.LIKES_TIMES_KEY_PREFIX;
import static com.personblog.common.constant.RedisKeys.LIKE_BIZ_KEY_PREFIX;
import static com.personblog.common.constant.TargetTypeConstant.*;
import static com.personblog.common.enums.BizCodeEnum.LIKE_ERROR;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BizLikeService implements LikeApi {
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ArticleLikeService articleLikeService;
    private final CommentLikeService commentLikeService;
    private final QuestionLikeService questionLikeService;
    private final AnswerLikeService answerLikeService;
    private final ArticleLikeMapper articleLikeMapper;
    private static final Map<String, LikeStrategy> likeStrategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        likeStrategyMap.put(ARTICLE, articleLikeService);
        likeStrategyMap.put(COMMENT, commentLikeService);
        likeStrategyMap.put(QUESTION, questionLikeService);
        likeStrategyMap.put(ANSWER, answerLikeService);
    }
    public boolean isLiked(Long targetId, Long userId,String targetType) {
        String key = LIKE_BIZ_KEY_PREFIX(targetType,targetId);
        Boolean result=redisTemplate.opsForSet().isMember(key, userId.toString());
        if(result==null||!redisTemplate.hasKey(key)){
            LikeStrategy likeStrategy = likeStrategyMap.get(targetType);
            result = likeStrategy.getIsLike(userId,targetId);
            if(result){
                // 使用 MQ 异步同步缓存
                SyncLikeCacheMessage messageDTO = SyncLikeCacheMessage.builder()
                        .targetType(targetType)
                        .build();
                rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, LIKE_SYNC_CACHE_KEY, messageDTO);
                log.info("消息已发送");
            }
        }
        return Boolean.TRUE.equals(result);
    }

    //获取单个点赞数
    public long getLikeCount(Long targetId,String targetType) {
        String peopleTimesKey = LIKE_BIZ_KEY_PREFIX(targetType,targetId);
        return redisTemplate.opsForSet().size(peopleTimesKey);
    }

    public Set<Long> batchIsLike(List<Long> targetIds, String targetType) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) return Collections.emptySet();

        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (Long targetId : targetIds) {
                String key = LIKE_BIZ_KEY_PREFIX(targetType,targetId);
                src.sIsMember(key,userId.toString());
            }
            return null;
        });
        int size = objects.size();
        return IntStream.range(0,size)
                .filter(i->(boolean) objects.get(i))
                .mapToObj(targetIds::get)
                .collect(Collectors.toSet());
    }

    public Map<Long, Long> getLikesTime(List<Long> targetIds, String targetType) {
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 使用包装器，而不是直接强制转换
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (Long targetId : targetIds) {
                String key = LIKE_BIZ_KEY_PREFIX(targetType,targetId);
                src.sCard(key);
            }
            return null;
        });
        Map<Long, Long> result = new HashMap<>(targetIds.size());
        for (int i = 0; i < targetIds.size(); i++) {
            Long targetId = targetIds.get(i);
            Object obj = objects.get(i);
            if (obj != null) {
                // 安全处理：zScore 返回 Double，但可能是 Number 子类
                if (obj instanceof Number) {
                    result.put(targetId, ((Number) obj).longValue());
                } else {
                    // 记录日志
                    log.info("获取点赞个数失败");
                }
            }
        }
        return result;
    }
    public void readLikesTimesAnd2DB(String TargetType, int maxSize) {
        String key = LIKES_TIMES_KEY_PREFIX + TargetType;
        Set<ZSetOperations.@NonNull TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().popMin(key, maxSize);
        List<LikeMessage> messages = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            String value = typedTuple.getValue();
            Double score = typedTuple.getScore();
            if (score == null||value==null) {
                continue;
            }
            messages.add(com.personblog.common.dto.MqMessage.Interaction.LikeMessage.builder()
                    .likeTimes(score.longValue())
                    .id(Long.valueOf(value))
                    .targetType(TargetType)
                    .build());
        }
        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE,LIKE_KEY,messages);
    }

    public LikedVO doLike(LikedDTO dto) {
        String key = LIKES_TIMES_KEY_PREFIX + dto.getTargetType();
        boolean isSuccess = dto.getIsLike() ? like(dto) : unLike(dto);
        if (!isSuccess) {
            throw new BizException(LIKE_ERROR);
        }
        String peopleTimesKey = LIKE_BIZ_KEY_PREFIX(dto.getTargetType(),dto.getTargetId());
        Long size = redisTemplate.opsForSet()
                .size(peopleTimesKey);
        redisTemplate.opsForZSet().add(key, dto.getTargetId().toString(), size);
        LikeSaveDBMessage dbMessageDTO = LikeSaveDBMessage.builder()
                .targetId(dto.getTargetId())
                .userId(UserContextHolder.getUserId())
                .targetType(dto.getTargetType())
                .isLike(dto.getIsLike())
                .authorId(dto.getAuthorId())
                .targetTitle(dto.getTargetTitle())
                .targetContent(dto.getTargetContent())
                .relatedId(dto.getRelatedId())
                .build();
        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE,LIKE_DB_KEY,dbMessageDTO);
        
        return LikedVO.builder().likes(size).build();
    }
    public void save2DB(LikeSaveDBMessage dtos) {
        LikeStrategy likeStrategy = likeStrategyMap.get(dtos.getTargetType());
        if (likeStrategy == null) {
            throw new BizException(LIKE_ERROR);
        }
        if (dtos.getIsLike()) {
            likeStrategy.saveLike(dtos.getUserId(), dtos.getTargetId());
        } else {
            likeStrategy.removeLike(dtos.getUserId(), dtos.getTargetId());
        }
    }

    public Page<MyLikeVO> getMyLikes(Long userId, Integer current, Integer size) {
        Page<MyLikeVO> page = new Page<>(current, size);
        return articleLikeMapper.selectMyLikes(page, userId);
    }

    //取消点赞
    private boolean unLike(LikedDTO dto) {
        Long userId = UserContextHolder.getUserId();
        String key = LIKE_BIZ_KEY_PREFIX(dto.getTargetType(),dto.getTargetId());
        Long remove = redisTemplate.opsForSet().remove(key, userId.toString());
        return remove != null && remove > 0;
    }

    //点赞
    private boolean like(LikedDTO dto) {
        Long userId = UserContextHolder.getUserId();
        String key = LIKE_BIZ_KEY_PREFIX(dto.getTargetType(),dto.getTargetId());
        Long add = redisTemplate.opsForSet().add(key, userId.toString());
        return add != null && add > 0;
    }
}
