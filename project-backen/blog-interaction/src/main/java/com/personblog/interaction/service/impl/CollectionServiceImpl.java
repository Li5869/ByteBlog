package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.interactionAPI.NotificationApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.Interaction.CollectionMessageDTO;
import com.personblog.common.dto.Notification.sse.NotificationMessageDTO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.interaction.dto.CollectionDTO;
import com.personblog.interaction.entity.Collection;
import com.personblog.interaction.mapper.CollectionMapper;
import com.personblog.interaction.service.CollectionService;
import com.personblog.interaction.vo.CollectionVO;
import com.personblog.interaction.vo.MyCollectionVO;
import com.personblog.interaction.vo.UserCollectionVO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.personblog.common.constant.RedisKeys.COLLECTION_TIMES_KEY_PREFIX;
import static com.personblog.common.constant.RedisKeys.COLLECTION_USER_KEY_PREFIX;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.common.enums.BizCodeEnum.ERROR_OPERATION;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.COLLECTION_KEY;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.INTERACTION_EXCHANGE;

/**
 * <p>
 * 收藏表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionServiceImpl extends ServiceImpl<CollectionMapper, Collection> implements CollectionService {
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationApi notificationApi;
    private final UseApi useApi;
    @Resource(name = "ArticleCountExecutor")
    private Executor executor;
    @Override
    public CollectionVO doCollection(CollectionDTO dto) {
        Boolean isCollection = dto.getIsCollection();
        boolean isSuccess = isCollection?unCollection(dto):didCollection(dto);
        if(!isSuccess) {
            throw new BizException(ERROR_OPERATION);
        }
        Long userId = UserContextHolder.getUserId();
        double s = isCollection?-1:1;
        Double score = redisTemplate.opsForZSet().incrementScore(COLLECTION_TIMES_KEY_PREFIX, dto.getArticleId().toString(), s);

        CollectionMessageDTO message = CollectionMessageDTO.builder()
                .articleId(dto.getArticleId())
                .collectionTimes(score.longValue())
                .userId(userId)
                .isCollection(isCollection)
                .delta((int)s)
                .build();

        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, COLLECTION_KEY, message);
        
        // 收藏时异步发送通知（取消收藏不通知）
        if (!isCollection) {
            Long articleId = dto.getArticleId();
            CompletableFuture.runAsync(() -> {
                try {
                    // 获取文章作者ID
                    Long authorId = dto.getArticleAuthorId();
                    if (authorId == null || authorId.equals(userId)) {
                        return;
                    }
                    
                    // 获取收藏者信息
                    List<UserDTO> users = useApi.getUserInfo(Collections.singleton(userId));
                    UserDTO sender = users.isEmpty() ? null : users.getFirst();
                    
                    // 构建通知消息
                    NotificationMessageDTO messageDTO = NotificationMessageDTO.builder()
                            .userId(authorId)
                            .actionType("collection")
                            .targetType(ARTICLE)
                            .targetId(articleId)
                            .senderId(userId)
                            .senderNickname(sender != null ? sender.getNickname() : "用户")
                            .senderAvatar(sender != null ? sender.getAvatar() : "")
                            .createdAt(LocalDateTime.now())
                            .build();
                    
                    // 保存通知到数据库
                    notificationApi.saveNotification(messageDTO);
                } catch (Exception e) {
                    log.error("保存收藏通知失败, articleId={}, userId={}", articleId, userId, e);
                }
            }, executor);
        }

        return CollectionVO.builder().collectionTimes(score.longValue()).build();
    }

    @Override
    public void save2DB(Long articleId, Long userId,Boolean isCollection) {
       if(isCollection){
           remove(new LambdaQueryWrapper<Collection>()
                   .eq(Collection::getUserId,userId)
                   .eq(Collection::getArticleId,articleId));
       }
       else {
           Collection collection = new Collection();
           collection.setUserId(userId);
           collection.setArticleId(articleId);
           save(collection);
       }
    }

    private boolean didCollection(CollectionDTO dto) {
        Long userId = UserContextHolder.getUserId();
        String key = COLLECTION_USER_KEY_PREFIX+dto.getArticleId();
        Long add = redisTemplate.opsForSet().add(key, userId.toString());
        return add != null && add > 0;
    }

    private boolean unCollection(CollectionDTO dto) {
        Long userId = UserContextHolder.getUserId();
        String key = COLLECTION_USER_KEY_PREFIX+dto.getArticleId();
        Long remove = redisTemplate.opsForSet().remove(key, userId.toString());
        return remove != null && remove > 0;
    }

    @Override
    public Page<MyCollectionVO> getMyCollections(Long userId, Integer current, Integer size) {
        int offset = (current - 1) * size;
        List<MyCollectionVO> records = baseMapper.selectMyCollections(userId, offset, size);
        long total = baseMapper.countByUserId(userId);
        Page<MyCollectionVO> page = new Page<>(current, size, total);
        page.setRecords(records);
        return page;
    }

    @Override
    public Page<UserCollectionVO> getUserCollections(Long userId, Integer current, Integer size) {
        int offset = (current - 1) * size;
        List<UserCollectionVO> records = baseMapper.selectUserCollections(userId, offset, size);
        long total = baseMapper.countByUserId(userId);
        Page<UserCollectionVO> page = new Page<>(current, size, total);
        page.setRecords(records);
        return page;
    }
}
