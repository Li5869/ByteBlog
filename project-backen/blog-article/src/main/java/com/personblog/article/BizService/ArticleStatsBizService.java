package com.personblog.article.BizService;

import com.personblog.api.articleAPI.ArticleMqAPI;
import com.personblog.article.entity.Article;
import com.personblog.article.service.IArticleService;
import com.personblog.common.dto.MqMessage.Interaction.BrowseHistoryMessage;
import com.personblog.common.dto.MqMessage.Interaction.CollectionMessage;
import com.personblog.common.dto.MqMessage.Interaction.LikeMessage;
import com.personblog.common.dto.MqMessage.user.UserLikeMessageDTO;
import com.personblog.common.utils.MultiLevelCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.personblog.common.constant.RedisKeys.ARTICLE_DETAIL;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.INTERACTION_EXCHANGE;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.USER_LIKE_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleStatsBizService implements ArticleMqAPI {
    private final RabbitTemplate rabbitTemplate;
    private final IArticleService articleService;
    private final MultiLevelCacheUtil cacheUtil;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLikeCount(List<LikeMessage> dtoList) {
        List<Long> articleIds = dtoList.stream()
                .map(com.personblog.common.dto.MqMessage.Interaction.LikeMessage::getId)
                .collect(Collectors.toList());

        Map<Long, Article> oldArticleMap =articleService.listByIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, article -> article));

        List<Article> list = new ArrayList<>(dtoList.size());
        List<UserLikeMessageDTO> userLikeMessages = new ArrayList<>();

        for (LikeMessage dto : dtoList) {
            Article article = new Article();
            article.setLikes(dto.getLikeTimes());
            article.setId(dto.getId());
            list.add(article);

            Article oldArticle = oldArticleMap.get(dto.getId());
            if (oldArticle != null && oldArticle.getAuthorId() != null) {
                long oldLikes = oldArticle.getLikes() != null ? oldArticle.getLikes() : 0L;
                long newLikes = dto.getLikeTimes() != null ? dto.getLikeTimes() : 0L;
                int delta = (int) (newLikes - oldLikes);

                if (delta != 0) {
                    userLikeMessages.add(UserLikeMessageDTO.builder()
                            .authorId(oldArticle.getAuthorId())
                            .delta(delta)
                            .build());
                }
            }
        }

        articleService.updateBatchById(list);

        if (!userLikeMessages.isEmpty()) {
            rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, USER_LIKE_KEY, userLikeMessages);
        }

        // 清理文章详情缓存
        evictArticleDetailCaches(articleIds);
    }

    @Override
    public void updateCollectionCount(CollectionMessage dto) {
        Article article = new Article();
        article.setId(dto.getArticleId());
        article.setCollections(dto.getCollectionTimes());
        articleService.updateById(article);
        // 清理文章详情缓存
        cacheUtil.evict(ARTICLE_DETAIL + dto.getArticleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBrowseCount(List<BrowseHistoryMessage> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }

        // 获取所有文章ID
        List<Long> articleIds = dtoList.stream()
                .map(BrowseHistoryMessage::getArticleId)
                .collect(Collectors.toList());

        // 批量查询数据库中的文章
        List<Article> dbArticles = articleService.listByIds(articleIds);
        Map<Long, Article> articleMap = dbArticles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));

        // 更新浏览量 = 数据库浏览量 + Redis增量
        List<Article> articles = dtoList.stream()
                .map(dto -> {
                    Article article = new Article();
                    article.setId(dto.getArticleId());
                    Article dbArticle = articleMap.get(dto.getArticleId());
                    long dbViews = (dbArticle != null) ? dbArticle.getViews() : 0L;
                    article.setViews(dbViews + dto.getViews());
                    return article;
                })
                .collect(Collectors.toList());
        articleService.updateBatchById(articles);

        // 清理文章详情缓存
        evictArticleDetailCaches(articleIds);
    }

    @Override
    @Async("CommentExecutor")
    public void updateCommentCount(Long articleId, int dealt) {
        articleService.lambdaUpdate()
                .eq(Article::getId,articleId)
                .setSql("comments = comments + {0}", dealt)
                .update();
    }
    /**
     * 批量清除文章详情的多级缓存
     * @param articleIds 文章ID列表
     */
    private void evictArticleDetailCaches(List<Long> articleIds) {
        for (Long articleId : articleIds) {
            cacheUtil.evict(ARTICLE_DETAIL + articleId);
        }
    }
}
