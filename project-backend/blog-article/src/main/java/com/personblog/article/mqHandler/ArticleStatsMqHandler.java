package com.personblog.article.mqHandler;

import com.personblog.api.adminAPI.TagApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.article.service.ICategoryService;
import com.personblog.common.dto.MqMessage.article.ArticleStatsMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

import static com.personblog.article.config.mqConfig.ArticleStatsMqConfig.ARTICLE_STATS_QUEUE;

/**
 * 文章统计更新 MQ 消费者
 * <p>
 * 异步处理文章创建/删除后的统计更新：
 * <ul>
 *   <li>更新用户文章数</li>
 *   <li>更新标签使用次数</li>
 *   <li>更新分类文章数</li>
 *   <li>清理标签缓存</li>
 * </ul>
 * 失败后进入死信队列，不会丢失消息。
 *
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = ARTICLE_STATS_QUEUE, containerFactory = "rabbitListenerContainerFactory")
public class ArticleStatsMqHandler {

    private final TagApi tagApi;
    private final ICategoryService categoryService;
    private final UseApi useApi;

    @RabbitHandler
    public void handleStatsUpdate(ArticleStatsMessage message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            Long userId = message.getUserId();
            Set<Long> tagIds = message.getTagIds();
            Set<Long> oldTagIds = message.getOldTagIds();
            Long categoryId = message.getCategoryId();
            Integer delta = message.getDelta();

            log.info("处理文章统计更新: userId={}, tagIds={}, oldTagIds={}, categoryId={}, delta={}", userId, tagIds, oldTagIds, categoryId, delta);

            // 1. 更新用户文章数
            if (userId != null) {
                useApi.updateArticlesCount(userId, delta);
            }

            // 2. 更新标签使用次数
            if (oldTagIds != null) {
                // 更新场景：通过差集计算新增和移除的标签
                Set<Long> toSub = new java.util.HashSet<>(oldTagIds);
                toSub.removeAll(tagIds);
                updateTagUseCount(toSub, -1);

                Set<Long> toAdd = new java.util.HashSet<>(tagIds);
                toAdd.removeAll(oldTagIds);
                updateTagUseCount(toAdd, 1);
            } else {
                // 创建/删除场景：所有标签统一 delta
                updateTagUseCount(tagIds, delta);
            }

            // 3. 更新分类文章数
            if (categoryId != null) {
                categoryService.updateCategoryCount(categoryId, delta);
            }

            // 4. 清理标签缓存
            tagApi.invalidateTagCache();

            // 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.debug("文章统计更新处理完成: articleId={}", message.getArticleId());
        } catch (Exception e) {
            log.error("处理文章统计更新失败, articleId={}, 消息将进入死信队列", message.getArticleId(), e);
            try {
                // nack 且不重新入队（进入死信队列）
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("basicNack 失败", ex);
            }
        }
    }

    /**
     * 更新标签使用次数
     */
    private void updateTagUseCount(Set<Long> tagIds, int delta) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        tagApi.batchUpdateTagUseCount(tagIds, delta);
    }
}
