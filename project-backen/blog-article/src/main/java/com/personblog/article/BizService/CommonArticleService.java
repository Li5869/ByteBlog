package com.personblog.article.BizService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.service.IArticleTagService;
import com.personblog.common.dto.MqMessage.search.SearchSyncMessageDTO;
import com.personblog.common.utils.MultiLevelCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

import static com.personblog.article.config.cachConfig.ArticleCacheConfig.articlePageCache;
import static com.personblog.article.config.cachConfig.ArticleCacheConfig.hotArticleCache;
import static com.personblog.common.constant.RedisKeys.ARTICLE_DETAIL;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.search.config.mqConfig.SearchMqConfig.SEARCH_EXCHANGE;
import static com.personblog.search.config.mqConfig.SearchMqConfig.SEARCH_SYNC_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonArticleService {
    private final IArticleTagService articleTagService;
    private final MultiLevelCacheUtil cacheUtil;
    private final RabbitTemplate rabbitTemplate;
    /**
     * 根据排序字段为wrapper添加排序条件
     * @param wrapper 查询条件构造器
     * @param orderBy 排序字段（views/likes/created_at）
     */
    public void applyOrderBy(LambdaQueryWrapper<Article> wrapper, String orderBy) {
        switch (orderBy != null ? orderBy : "created_at") {
            case "views":
                wrapper.orderByDesc(Article::getViews);
                break;
            case "likes":
                wrapper.orderByDesc(Article::getLikes);
                break;
            default:
                wrapper.orderByDesc(Article::getCreatedAt);
                break;
        }
    }
    /**
     * 根据文章ID查询关联的标签ID集合
     * @param articleId 文章ID
     * @return 标签ID集合
     */
    public Set<Long> getTagIdsByArticleId(Long articleId) {
        return articleTagService.lambdaQuery()
                .eq(ArticleTag::getArticleId, articleId)
                .select(ArticleTag::getTagId)
                .list().stream()
                .map(ArticleTag::getTagId)
                .collect(Collectors.toSet());
    }

    public void removeArticleCache(Long articleId, String s) {
        // 清除相关缓存
        hotArticleCache.invalidateAll();
        articlePageCache.invalidateAll();
        cacheUtil.evict(ARTICLE_DETAIL + articleId);
        log.info(s, articleId);
    }
    /**
     * 发送搜索同步消息到 MQ
     *
     * @param operation  操作类型：sync-同步，delete-删除
     * @param articleId  文章ID
     */
   public void sendSearchSyncMessage(String operation, Long articleId) {
        try {
            SearchSyncMessageDTO message = SearchSyncMessageDTO.builder()
                    .operation(operation)
                    .targetType(ARTICLE)
                    .targetId(articleId)
                    .build();
            rabbitTemplate.convertAndSend(SEARCH_EXCHANGE, SEARCH_SYNC_KEY, message);
            log.info("发送文章搜索同步消息成功: operation={}, articleId={}", operation, articleId);
        } catch (Exception e) {
            log.error("发送文章搜索同步消息失败: operation={}, articleId={}", operation, articleId, e);
        }
    }
}
