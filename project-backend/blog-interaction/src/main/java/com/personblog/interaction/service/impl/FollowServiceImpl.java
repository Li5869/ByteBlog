package com.personblog.interaction.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.interactionAPI.FollowApi;
import com.personblog.common.dto.MqMessage.Interaction.FollowMessage;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.MultiLevelCacheUtil;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.interaction.dto.FollowDTO;
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

import static com.personblog.common.constant.RedisKeys.USER_AUTHOR;
import static com.personblog.common.constant.RedisKeys.USER_INFO;
import static com.personblog.common.enums.BizCodeEnum.FOLLOW_ERROR;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.*;
import static com.personblog.interaction.constant.RedisKeys.USER_FOLLOW;

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
    public List<Long> getFollowerIds(Long userId) {
        List<Follow> followers = lambdaQuery()
                .eq(Follow::getFollowingId, userId)
                .list();
        if (CollectionUtil.isEmpty(followers)) {
            return List.of();
        }
        List<Follow> followings = lambdaQuery()
                .eq(Follow::getFollowerId, userId)
                .list();
        if (CollectionUtil.isEmpty(followings)) {
            return List.of();
        }
        Set<Long> followingIdSet = followings.stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toSet());
        return followers.stream()
                .map(Follow::getFollowerId)
                .filter(followingIdSet::contains)
                .toList();
    }

    @Override
    public FollowVO doFollow(FollowDTO dto) {
        Long userId = UserContextHolder.getUserId();
        boolean isSuccess = dto.getIsFollow() ? unFollow(dto.getFollowingId(), userId) : followed(dto.getFollowingId(), userId);
        if(!isSuccess) throw new BizException(FOLLOW_ERROR);
        
        FollowMessage messageDTO = FollowMessage.builder()
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
        // 从缓存中获取当前用户的全部关注列表
        Set<String> cachedMembers = redisTemplate.opsForSet().members(key);
        if (CollectionUtil.isNotEmpty(cachedMembers)) {
            // 缓存命中：从中筛选出入参中已关注的用户 ID
            Set<String> followingIdStrSet = followingIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
            return cachedMembers.stream()
                    .filter(followingIdStrSet::contains)
                    .map(Long::valueOf)
                    .toList();
        }
        // 缓存未命中：查库获取当前用户的所有关注记录
        List<Follow> allFollows = lambdaQuery()
                .eq(Follow::getFollowerId, userId)
                .list();

        if (CollectionUtil.isEmpty(allFollows)) {
            return List.of();
        }
        // 将关注者 ID 写入缓存，设置 30 分钟过期
        List<String> allFollowingIds = allFollows.stream()
                .map(s -> s.getFollowingId().toString())
                .toList();
        redisTemplate.opsForSet().add(key, allFollowingIds.toArray(new String[0]));
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        // 从全量关注列表中筛选入参中已关注的 ID 并返回
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
