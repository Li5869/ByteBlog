package com.personblog.interaction.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.interactionAPI.FollowApi;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.MultiLevelCacheUtil;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.interaction.dto.FollowDTO;
import com.personblog.interaction.dto.MqMessage.FollowMessageDTO;
import com.personblog.interaction.entity.Follow;
import com.personblog.interaction.mapper.FollowMapper;
import com.personblog.interaction.service.FollowService;
import com.personblog.interaction.vo.FollowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.personblog.common.config.mqConfig.InteractionMqConfig.*;
import static com.personblog.common.constant.RedisKeys.*;
import static com.personblog.common.enums.BizCodeEnum.FOLLOW_ERROR;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService, FollowApi {
    private final RabbitTemplate rabbitTemplate;
    private final MultiLevelCacheUtil cacheUtil;
    private final StringRedisTemplate redisTemplate;
    @Override
    public List<Long> getFollowingIds(Long userId) {
        List<Follow> list = lambdaQuery()
                .eq(Follow::getFollowerId, userId)
                .list();
        if (CollectionUtil.isEmpty(list)) {
            return List.of();
        }
        return list.stream()
                .map(Follow::getFollowingId)
                .toList();
    }

    @Override
    public FollowVO doFollow(FollowDTO dto) {
        Long userId = UserContextHolder.getUserId();
        boolean isSuccess = dto.getIsFollow() ? unFollow(dto.getFollowingId(), userId) : followed(dto.getFollowingId(), userId);
        if(!isSuccess) throw new BizException(FOLLOW_ERROR);
        
        FollowMessageDTO messageDTO = FollowMessageDTO.builder()
                .followingId(dto.getFollowingId())
                .isFollow(dto.getIsFollow())
                .followerId(userId)
                .build();
        
        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, FOLLOW_KEY, messageDTO);
        if (!dto.getIsFollow()) {
            rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, FOLLOW_NOTIFICATION_KEY, messageDTO);
        }
        
        String authorCacheKey = USER_AUTHOR + dto.getFollowingId();
        cacheUtil.evict(authorCacheKey);

        // 关注数变化，清理当前用户的用户信息缓存
        cacheUtil.evict(USER_INFO + userId);

        String followCacheKey = USER_FOLLOW + userId;
        redisTemplate.delete(followCacheKey);
        
        return FollowVO.builder().isFollow(!dto.getIsFollow()).build();
    }

    @Override
    public List<Long> checkBatchFollowStatus(List<Long> followingIds) {
        if (followingIds == null || followingIds.isEmpty()) {
            return List.of();
        }
        
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return List.of();
        }
        
        String key = USER_FOLLOW + userId;
        
        Set<String> cachedMembers = redisTemplate.opsForSet().members(key);
        if (CollectionUtil.isNotEmpty(cachedMembers)) {
            Set<String> followingIdStrSet = followingIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
            return cachedMembers.stream()
                    .filter(followingIdStrSet::contains)
                    .map(Long::valueOf)
                    .toList();
        }
        
        List<Follow> allFollows = lambdaQuery()
                .eq(Follow::getFollowerId, userId)
                .list();
        
        if (CollectionUtil.isEmpty(allFollows)) {
            return List.of();
        }
        
        List<String> allFollowingIds = allFollows.stream()
                .map(s -> s.getFollowingId().toString())
                .toList();
        redisTemplate.opsForSet().add(key, allFollowingIds.toArray(new String[0]));
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        
        Set<Long> allFollowingIdSet = allFollows.stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toSet());
        
        return followingIds.stream()
                .filter(allFollowingIdSet::contains)
                .toList();
    }

    private boolean followed(Long followingId, Long userId) {
        Follow follow = new Follow();
        follow.setFollowingId(followingId);
        follow.setFollowerId(userId);
        return save(follow);
    }

    private boolean unFollow(Long followingId, Long userId) {
       return remove(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowingId, followingId)
                .eq(Follow::getFollowerId, userId));
    }
}
