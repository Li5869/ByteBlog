package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.interactionAPI.BrowseHistoryApi;
import com.personblog.common.dto.MqMessage.Interaction.BrowseHistoryMessage;
import com.personblog.interaction.entity.BrowseHistory;
import com.personblog.interaction.mapper.BrowseHistoryMapper;
import com.personblog.interaction.service.BrowseHistoryService;
import com.personblog.interaction.vo.BrowseHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.personblog.common.constant.RedisKeys.*;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.BROWSE_HISTORY_KEY;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.INTERACTION_EXCHANGE;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> implements BrowseHistoryService, BrowseHistoryApi {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;


    @Override
    public void recordBrowse(Long userId, Long articleId) {
            redisTemplate.opsForHash().increment(BROWSE_COUNT_KEY, articleId.toString(), 1);
            if (userId != null && userId > 0) {
                String historyKey = BROWSE_HISTORY_KEY_PREFIX + userId;
                long timestamp = System.currentTimeMillis();
                redisTemplate.opsForZSet().add(historyKey, articleId.toString(), timestamp);
                redisTemplate.opsForSet().add(BROWSE_ACTIVE_USERS, userId.toString());
        }
    }

    @Override
    public Page<BrowseHistoryVO> getUserBrowseHistory(Long userId, Integer current, Integer size) {
        int pageNum = (current == null || current <= 0) ? 1 : current;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 50);
        
        long offset = (long) (pageNum - 1) * pageSize;
        String key = BROWSE_HISTORY_KEY_PREFIX+userId;
        List<BrowseHistoryVO> voList = baseMapper.selectUserBrowseHistory(userId, offset, pageSize);
        long total = baseMapper.countByUserId(userId);
        
        Page<BrowseHistoryVO> resultPage = new Page<>(pageNum, pageSize, total);
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public void syncBrowseHistory2DB() {
        Map<Object, Object> browseCountMap = redisTemplate.opsForHash().entries(BROWSE_COUNT_KEY);
        if (browseCountMap.isEmpty()) {
            log.info("没有需要同步的浏览数据");
            return;
        }
        
        List<BrowseHistoryMessage> countMessageList = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : browseCountMap.entrySet()) {
            String articleIdStr = (String) entry.getKey();
            String countStr = (String) entry.getValue();
            if (articleIdStr != null && countStr != null) {
                Long articleId = Long.valueOf(articleIdStr);
                Long views = Long.valueOf(countStr);
                countMessageList.add(BrowseHistoryMessage.builder()
                        .articleId(articleId)
                        .views(views)
                        .build());
            }
        }
        
        if (!countMessageList.isEmpty()) {
            rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, BROWSE_HISTORY_KEY, countMessageList);
            redisTemplate.delete(BROWSE_COUNT_KEY);
            log.info("浏览计数同步完成，共 {} 条记录", countMessageList.size());
        }
        
        Set<String> activeUsers = redisTemplate.opsForSet().members(BROWSE_ACTIVE_USERS);
        if (activeUsers != null && !activeUsers.isEmpty()) {
            List<BrowseHistoryMessage> historyMessageList = new ArrayList<>();
            List<String> keysToDelete = new ArrayList<>();
            
            for (String userIdStr : activeUsers) {
                Long userId;
                try {
                    userId = Long.valueOf(userIdStr);
                } catch (NumberFormatException e) {
                    log.warn("无效的用户ID: {}", userIdStr);
                    continue;
                }
                
                String historyKey = BROWSE_HISTORY_KEY_PREFIX + userId;
                keysToDelete.add(historyKey);
                
                Set<ZSetOperations.TypedTuple<String>> browseRecords = 
                        redisTemplate.opsForZSet().rangeWithScores(historyKey, 0, -1);
                
                if (browseRecords != null && !browseRecords.isEmpty()) {
                    for (ZSetOperations.TypedTuple<String> tuple : browseRecords) {
                        if (tuple.getValue() != null && tuple.getScore() != null) {
                            Long articleId = Long.valueOf(tuple.getValue());
                            long timestamp = tuple.getScore().longValue();
                            LocalDateTime browseTime = LocalDateTime.ofEpochSecond(
                                    timestamp / 1000, 0, ZoneOffset.ofHours(8));
                            
                            historyMessageList.add(BrowseHistoryMessage.builder()
                                    .userId(userId)
                                    .articleId(articleId)
                                    .browseTime(browseTime)
                                    .build());
                        }
                    }
                }
            }
            
            if (!historyMessageList.isEmpty()) {
                rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, BROWSE_HISTORY_KEY, historyMessageList);
                log.info("浏览历史同步完成，共 {} 条记录", historyMessageList.size());
            }
            
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
            redisTemplate.delete(BROWSE_ACTIVE_USERS);
        }
    }
}
