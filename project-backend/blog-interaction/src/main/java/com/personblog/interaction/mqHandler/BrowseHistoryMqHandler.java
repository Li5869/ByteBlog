package com.personblog.interaction.mqHandler;

import com.personblog.api.articleAPI.ArticleMqAPI;
import com.personblog.common.dto.MqMessage.Interaction.BrowseHistoryMessage;
import com.personblog.interaction.entity.BrowseHistory;
import com.personblog.interaction.mapper.BrowseHistoryMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.BROWSE_HISTORY_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrowseHistoryMqHandler {

    private final ArticleMqAPI articleAPI;
    private final BrowseHistoryMapper browseHistoryMapper;

    @RabbitListener(queues = BROWSE_HISTORY_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleBrowseHistoryMessage(List<BrowseHistoryMessage> dtoList, Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("开始处理浏览历史消息，共 {} 条记录", dtoList.size());
            
            List<BrowseHistoryMessage> countList = new ArrayList<>();
            List<BrowseHistory> historyList = new ArrayList<>();
            
            for (BrowseHistoryMessage dto : dtoList) {
                if (dto.getViews() != null && dto.getViews() > 0) {
                    countList.add(dto);
                } else if (dto.getUserId() != null && dto.getArticleId() != null && dto.getBrowseTime() != null) {
                    BrowseHistory browseHistory = new BrowseHistory();
                    browseHistory.setUserId(dto.getUserId());
                    browseHistory.setArticleId(dto.getArticleId());
                    browseHistory.setBrowseAt(dto.getBrowseTime());
                    historyList.add(browseHistory);
                }
            }
            
            if (!countList.isEmpty()) {
                articleAPI.updateBrowseCount(countList);
                log.info("文章浏览数更新完成，共 {} 条", countList.size());
            }
            
            if (!historyList.isEmpty()) {
                browseHistoryMapper.batchInsertOrUpdate(historyList);
                log.info("浏览历史保存完成，共 {} 条", historyList.size());
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("浏览历史处理失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
