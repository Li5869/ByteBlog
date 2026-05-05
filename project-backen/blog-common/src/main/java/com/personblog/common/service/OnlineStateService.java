package com.personblog.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.personblog.common.constant.RedisKeys.ONLINE_USERS;
import static com.personblog.common.constant.RedisKeys.getUserOnlineDetailKey;

/**
 * 用户在线状态服务
 * 基于 Redis Set 实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStateService{

    private final StringRedisTemplate redisTemplate;

    private static final int ONLINE_EXPIRE_SECONDS = 300;
    // 用户在线状态过期时间：5分钟
    public void userOnline(Long userId, Long loginTime) {
        redisTemplate.opsForSet().add(ONLINE_USERS, userId.toString());

        String detailKey = getUserOnlineDetailKey(userId);
        String detailValue = String.format("{\"loginTime\":%d,\"device\":\"web\"}", loginTime);
        redisTemplate.opsForValue().set(detailKey, detailValue, ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("[Online] 用户上线: userId={}", userId);
    }

    public void userOffline(Long userId) {
        redisTemplate.opsForSet().remove(ONLINE_USERS, userId.toString());

        String detailKey = getUserOnlineDetailKey(userId);
        redisTemplate.delete(detailKey);

        log.info("[Online] 用户下线: userId={}", userId);
    }

    public void heartbeat(Long userId) {
        String detailKey = getUserOnlineDetailKey(userId);

        Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS, userId.toString());
        // 如果用户在线，续期在线状态
        if (Boolean.TRUE.equals(isMember)) {
            redisTemplate.expire(detailKey, ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("[Online] 心跳续期: userId={}", userId);
        }
        // 如果用户不在线，检查是否有在线状态记录
        // 如果有，恢复在线状态
        else {
            String detailValue = redisTemplate.opsForValue().get(detailKey);
            if (detailValue != null) {
                redisTemplate.opsForSet().add(ONLINE_USERS, userId.toString());
                redisTemplate.expire(detailKey, ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                log.warn("[Online] 心跳恢复在线状态: userId={}", userId);
            }
        }
    }

    public boolean isOnline(Long userId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS, userId.toString());
        return Boolean.TRUE.equals(isMember);
    }

    public Map<Long, Boolean> batchGetOnlineStatus(List<Long> userIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (Long userId : userIds) {
                src.sIsMember(ONLINE_USERS, userId.toString());
            }
            return null;
        });
        int size = objects.size();
        IntStream.range(0, size).forEach(i -> {
            Long userId = userIds.get(i);
            Boolean isMember = Boolean.TRUE.equals((objects.get(i)));
            result.put(userId, isMember);
        });
        return result;
    }
    // 获取所有在线用户
    public Set<String> getAllOnlineUsers() {
        return redisTemplate.opsForSet().members(ONLINE_USERS);
    }
    // 获取在线用户数量
    public Long getOnlineCount() {
        Long count = redisTemplate.opsForSet().size(ONLINE_USERS);
        return count != null ? count : 0L;
    }
    // 获取用户在线状态详情
    public String getUserOnlineDetail(Long userId) {
        String detailKey = getUserOnlineDetailKey(userId);
        return redisTemplate.opsForValue().get(detailKey);
    }
    // 清理所有在线状态
    public void clearAllOnline() {
        Set<String> onlineUsers = getAllOnlineUsers();
        if (onlineUsers != null && !onlineUsers.isEmpty()) {
            onlineUsers.forEach(userId -> {
                String detailKey = getUserOnlineDetailKey(Long.parseLong(userId));
                redisTemplate.delete(detailKey);
            });
        }
        redisTemplate.delete(ONLINE_USERS);
        log.info("[Online] 清理所有在线状态");
    }
}
