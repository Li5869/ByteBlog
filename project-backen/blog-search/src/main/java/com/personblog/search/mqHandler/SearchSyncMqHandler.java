package com.personblog.search.mqHandler;

import com.personblog.common.config.mqConfig.SearchMqConfig;
import com.personblog.common.dto.Search.SearchSyncMessageDTO;
import com.personblog.search.service.DeleteSearchService;
import com.personblog.search.service.SearchSyncService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.common.constant.TargetTypeConstant.*;

/**
 * 搜索同步 MQ 消息处理器
 * 监听搜索同步队列，异步处理索引同步和删除操作
 * 
 * 解耦说明：
 * - 业务模块（如 ColumnServiceImpl）不再直接依赖 SearchSyncApi
 * - 而是通过 MQ 发送消息，由本处理器异步处理
 * - 避免了业务模块和搜索模块之间的循环依赖
 *
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchSyncMqHandler {

    private final SearchSyncService searchSyncService;
    private final DeleteSearchService deleteSearchService;

    /**
     * 监听搜索同步队列
     */
    @RabbitListener(queues = SearchMqConfig.SEARCH_SYNC_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleSearchSyncMessage(SearchSyncMessageDTO message, Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到搜索同步消息: operation={}, targetType={}, targetId={}",
                message.getOperation(), message.getTargetType(), message.getTargetId());

        try {
            String operation = message.getOperation();
            String targetType = message.getTargetType();
            Long targetId = message.getTargetId();

            // 根据操作类型分发处理
            if (SearchMqConfig.OPERATION_SYNC.equals(operation)) {
                handleSync(targetType, targetId);
            } else if (SearchMqConfig.OPERATION_DELETE.equals(operation)) {
                handleDelete(targetType, targetId);
            } else {
                log.warn("未知的操作类型: {}", operation);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("搜索同步消息处理失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理同步操作
     */
    private void handleSync(String targetType, Long targetId) {
        switch (targetType) {
            case ARTICLE -> searchSyncService.syncArticle(targetId);
            case QUESTION -> searchSyncService.syncQuestion(targetId);
            case AUTHOR -> searchSyncService.syncAuthor(targetId);
            case COLUMN -> searchSyncService.syncColumn(targetId);
            default -> log.warn("未知的目标类型: {}", targetType);
        }
    }

    /**
     * 处理删除操作
     */
    private void handleDelete(String targetType, Long targetId) {
        switch (targetType) {
            case ARTICLE -> deleteSearchService.deleteArticle(targetId);
            case QUESTION -> deleteSearchService.deleteQuestion(targetId);
            case AUTHOR -> deleteSearchService.deleteAuthor(targetId);
            case COLUMN -> deleteSearchService.deleteColumn(targetId);
            default -> log.warn("未知的目标类型: {}", targetType);
        }
    }
}
