package com.personblog.common.dto.MqMessage.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索同步消息 DTO
 * 用于通过 MQ 异步同步搜索索引，解耦业务模块和搜索模块
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSyncMessageDTO {

    /**
     * 操作类型：sync-同步，delete-删除
     */
    private String operation;

    /**
     * 目标类型：article-文章，question-问题，author-作者，column-专栏
     */
    private String targetType;

    /**
     * 目标ID
     */
    private Long targetId;
}
