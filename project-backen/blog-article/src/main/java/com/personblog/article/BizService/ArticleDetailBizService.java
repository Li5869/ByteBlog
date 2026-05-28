package com.personblog.article.BizService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.adminAPI.TagApi;
import com.personblog.api.adminAPI.vo.TagVO;
import com.personblog.api.interactionAPI.BrowseHistoryApi;
import com.personblog.api.interactionAPI.LikeApi;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.entity.Category;
import com.personblog.article.service.IArticleService;
import com.personblog.article.service.IArticleTagService;
import com.personblog.article.service.ICategoryService;
import com.personblog.article.vo.Article.ArticleInteractionVO;
import com.personblog.article.vo.Article.ArticleMetadataVO;
import com.personblog.article.vo.Article.RelatedArticleVO;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.MultiLevelCacheUtil;
import com.personblog.common.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.personblog.common.constant.RedisKeys.*;
import static com.personblog.common.constant.StatusConstant.APPROVED;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.common.enums.BizCodeEnum.NOT_ARTICLE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleDetailBizService {

    private final ICategoryService categoryService;
    private final IArticleTagService articleTagService;
    private final TagApi tagApi;
    private final LikeApi likeApi;
    private final StringRedisTemplate redisTemplate;
    private final BrowseHistoryApi browseHistoryApi;
    private final MultiLevelCacheUtil cacheUtil;
    private final IArticleService articleService;
    private final CommonArticleService commonArticleService;
    /**
     * 获取文章详情
     * 重要：必须创建新对象副本再修改，避免污染缓存中的原始对象
     */
    public ArticleMetadataVO getArticleMetadata(Long id) {
        String cacheKey = ARTICLE_METADATA + id;
        browseHistoryApi.recordBrowse(UserContextHolder.getUserId(),id);
        // 从缓存获取文章基础信息（含正文、分类、标签），不包含互动数据
        return cacheUtil.get(
                cacheKey,
                key -> loadArticleMetadataFromDB(id),
                600,
                120,
                ArticleMetadataVO.class
        );
    }
    public ArticleInteractionVO getArticleInteraction(Long id) {
        Long userId = UserContextHolder.getUserId() == null ? -1 : UserContextHolder.getUserId();

        // Pipeline 批量查询实时互动状态（isCollected、isLiked、浏览量增量、点赞数）
        InteractionQueryResult interaction = queryArticleInteractions(id, userId);

        Article article = articleService.getById(id);
        if (article == null) {
            throw new BizException(NOT_ARTICLE);
        }

        ArticleInteractionVO vo = new ArticleInteractionVO();
        vo.setId(id);
        vo.setIsLiked(interaction.isLiked());
        vo.setIsCollected(interaction.isCollected());
        vo.setTotalLikes(interaction.likeCount());

        // 浏览量 = 数据库基础值 + Redis 实时增量
        long baseViews = article.getViews() != null ? article.getViews() : 0L;
        if (interaction.browseCount() != null) {
            vo.setViews(baseViews + interaction.browseCount);
        } else {
            vo.setViews(baseViews);
        }

        vo.setComments(article.getComments() != null ? article.getComments() : 0L);
        vo.setCollections(article.getCollections() != null ? article.getCollections() : 0L);

        return vo;
    }

    public List<RelatedArticleVO> getRelatedArticles(Long articleId, Integer limit) {
        int size = commonArticleService.normalizeLimitSize(limit, 3, 10);

        Article currentArticle = articleService.getById(articleId);
        if (currentArticle == null) {
            return Collections.emptyList();
        }

        Set<Long> tagIds = commonArticleService.getTagIdsByArticleId(articleId);

        Set<Long> relatedArticleIds = new HashSet<>();
        if (!tagIds.isEmpty()) {
            relatedArticleIds.addAll(articleTagService.lambdaQuery()
                    .in(ArticleTag::getTagId, tagIds)
                    .ne(ArticleTag::getArticleId, articleId)
                    .select(ArticleTag::getArticleId)
                    .list().stream()
                    .map(ArticleTag::getArticleId)
                    .collect(Collectors.toSet()));
        }

        if (relatedArticleIds.size() < size && currentArticle.getCategoryId() != null) {
            int remainSize = size - relatedArticleIds.size();
            LambdaQueryWrapper<Article> categoryWrapper = new LambdaQueryWrapper<>();
            categoryWrapper.eq(Article::getCategoryId, currentArticle.getCategoryId())
                    .ne(Article::getId, articleId)
                    .eq(Article::getStatus, 1)
                    .eq(Article::getIsDeleted, false)
                    .eq(Article::getReview, APPROVED)
                    .select(Article::getId);
            List<Long> sameCategoryIds = articleService.page(new Page<>(1, remainSize), categoryWrapper).getRecords()
                    .stream()
                    .map(Article::getId)
                    .toList();

            relatedArticleIds.addAll(sameCategoryIds);
        }

        if (relatedArticleIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Article> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.in(Article::getId, relatedArticleIds)
                .eq(Article::getStatus, 1)
                .eq(Article::getIsDeleted, false)
                .eq(Article::getReview, APPROVED)
                .orderByDesc(Article::getViews);
        List<Article> articles = articleService.page(new Page<>(1, size), resultWrapper).getRecords();

        return articles.stream().map(article -> {
            RelatedArticleVO vo = new RelatedArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setCover(article.getCover());
            return vo;
        }).collect(Collectors.toList());
    }



    /**
     * 从数据库加载文章基础信息（含正文、分类、标签）
     * ArticleMetadataVO 包含 content 字段，统一缓存，避免分拆加载的网络开销
     */
    private ArticleMetadataVO loadArticleMetadataFromDB(Long id) {
        Article article = articleService.getById(id);
        if (article == null) {
            throw new BizException(NOT_ARTICLE);
        }

        Long userId = UserContextHolder.getUserId();

        if (!APPROVED.equals(article.getReview())) {
            if (userId == null || !userId.equals(article.getAuthorId())) {
                log.warn("用户尝试访问未审核通过的文章, articleId={}, userId={}", id, userId);
                throw new BizException(NOT_ARTICLE);
            }
        }
        ArticleMetadataVO vo = BeanUtil.copyProperties(article, ArticleMetadataVO.class);

        // 查询分类信息
        Category one = categoryService.lambdaQuery()
                .eq(Category::getId, article.getCategoryId())
                .one();
        ArticleMetadataVO.CategoryInfo categoryInfoVO = BeanUtil.copyProperties(one, ArticleMetadataVO.CategoryInfo.class);

        // 查询标签信息
        Set<Long> tagIds = commonArticleService.getTagIdsByArticleId(article.getId());
        if (CollectionUtil.isNotEmpty(tagIds)) {
            List<TagVO> tagVOs = tagApi.getTagsByIds(tagIds);
            if (CollectionUtil.isNotEmpty(tagVOs)) {
                vo.setTags(tagVOs.stream()
                        .map(tag -> BeanUtil.copyProperties(tag, ArticleMetadataVO.TagInfo.class)).toList());
            }
        }
        vo.setCategory(categoryInfoVO);
        return vo;
    }

    /**
     * 使用 Redis Pipeline 批量查询文章互动数据
     * 已登录：查询收藏状态 + 浏览量增量 + 点赞数（3 条命令合并）
     * 未登录：仅查询浏览量增量 + 点赞数（2 条命令合并），跳过用户相关状态
     * Pipeline 命令顺序（已登录）：
     *   0. SISMEMBER collections:set:articleId:{id} {userId}  - 是否已收藏
     *   1. HGET browse:count {id}                             - 浏览量增量
     *   2. SCARD likes:set:article:{id}                       - 实时点赞数
     * Pipeline 命令顺序（未登录）：
     *   0. HGET browse:count {id}                             - 浏览量增量
     *   1. SCARD likes:set:article:{id}                       - 实时点赞数
     */
    private InteractionQueryResult queryArticleInteractions(Long articleId, Long userId) {
        String collectionKey = COLLECTION_USER_KEY_PREFIX + articleId;
        String likeKey = LIKE_BIZ_KEY_PREFIX(ARTICLE, articleId);
        boolean loggedIn = userId != -1;

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            if (loggedIn) {
                src.sIsMember(collectionKey, String.valueOf(userId));
            }
            src.hGet(BROWSE_COUNT_KEY, articleId.toString());
            src.sCard(likeKey);
            return null;
        });

        // Pipeline 返回原生类型：SISMEMBER→Boolean, HGET→String, SCARD→Long
        int offset = loggedIn ? 1 : 0;
        boolean isCollected = loggedIn ? (Boolean) results.getFirst() : false;
        boolean isLiked = loggedIn && likeApi.isLiked(articleId, userId, ARTICLE);
        String browseCountStr = (String) results.get(offset);
        Long browseCount = browseCountStr != null ? Long.parseLong(browseCountStr) : null;
        long likeCount = (Long) results.get(offset + 1);

        return new InteractionQueryResult(isCollected, isLiked, browseCount, likeCount);
    }
    /**
     * 文章互动数据查询结果（Pipeline 批量返回）
     */
    private record InteractionQueryResult(
            boolean isCollected,
            boolean isLiked,
            Long browseCount,
            long likeCount
    ) {}
}
