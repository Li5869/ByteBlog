package com.personblog.article.BizService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.service.IArticleService;
import com.personblog.article.service.IArticleTagService;
import com.personblog.common.dto.MqMessage.search.SearchSyncMessageDTO;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.MultiLevelCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.personblog.article.config.cachConfig.ArticleCacheConfig.articlePageCache;
import static com.personblog.article.config.cachConfig.ArticleCacheConfig.hotArticleCache;
import static com.personblog.common.constant.RedisKeys.ARTICLE_DETAIL;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.common.enums.BizCodeEnum.NOT_ARTICLE;
import static com.personblog.common.enums.BizCodeEnum.NO_POWER;
import static com.personblog.search.config.mqConfig.SearchMqConfig.SEARCH_EXCHANGE;
import static com.personblog.search.config.mqConfig.SearchMqConfig.SEARCH_SYNC_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonArticleService {
    private final IArticleService articleService;
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

    /**
     * 校验文章存在性及作者权限
     * @param articleId 文章ID
     * @param userId 当前用户ID
     * @return 校验通过的文章实体
     * @throws BizException 文章不存在或无权限
     */
    public Article getAndValidateArticle(Long articleId, Long userId) {
        Article article = articleService.getById(articleId);
        if (article == null || Boolean.TRUE.equals(article.getIsDeleted())) {
            throw new BizException(NOT_ARTICLE);
        }
        if (!Objects.equals(article.getAuthorId(), userId)) {
            throw new BizException(NO_POWER);
        }
        return article;
    }

    /**
     * 校验文章存在性（仅检查文章是否存在）
     * @param articleId 文章ID
     * @return 校验通过的文章实体
     * @throws BizException 文章不存在
     */
    public Article getArticleIfExists(Long articleId) {
        Article article = articleService.getById(articleId);
        if (article == null || Boolean.TRUE.equals(article.getIsDeleted())) {
            throw new BizException(NOT_ARTICLE);
        }
        return article;
    }

    /**
     * 批量清除文章详情缓存
     * @param articleIds 文章ID列表
     */
    public void evictArticleDetailCaches(List<Long> articleIds) {
        for (Long articleId : articleIds) {
            cacheUtil.evict(ARTICLE_DETAIL + articleId);
        }
    }

    /**
     * 规范化分页参数
     * @param current 当前页码
     * @param size 每页大小
     * @param defaultSize 默认每页大小
     * @param maxSize 最大每页大小
     * @return 规范化后的分页参数 [current, size]
     */
    public int[] normalizePageParams(Integer current, Integer size, int defaultSize, int maxSize) {
        int normalizedCurrent = (current == null || current <= 0) ? 1 : current;
        int normalizedSize = (size == null || size <= 0) ? defaultSize : Math.min(size, maxSize);
        return new int[]{normalizedCurrent, normalizedSize};
    }

    /**
     * 规范化限制数量参数
     * @param size 限制数量
     * @param defaultSize 默认数量
     * @param maxSize 最大数量
     * @return 规范化后的数量
     */
    public int normalizeLimitSize(Integer size, int defaultSize, int maxSize) {
        return (size == null || size <= 0) ? defaultSize : Math.min(size, maxSize);
    }
}
