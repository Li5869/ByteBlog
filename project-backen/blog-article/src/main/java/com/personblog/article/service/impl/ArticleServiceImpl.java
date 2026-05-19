package com.personblog.article.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.api.AIAPI.AiArticleDraftApi;
import com.personblog.api.AIwritingAPI.WritingTaskApi;
import com.personblog.api.adminAPI.TagApi;
import com.personblog.api.adminAPI.TagVO;
import com.personblog.api.articleAPI.ArticleInfoAPI;
import com.personblog.api.interactionAPI.BrowseHistoryApi;
import com.personblog.api.interactionAPI.FollowApi;
import com.personblog.api.interactionAPI.LikeApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.article.dto.article.AdminArticleQueryDTO;
import com.personblog.article.dto.article.ArticlePublishDTO;
import com.personblog.article.dto.article.ArticleQueryDTO;
import com.personblog.article.dto.message.ArticleStatsMessage;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.entity.Category;
import com.personblog.article.mapper.ArticleMapper;
import com.personblog.article.service.IArticleService;
import com.personblog.article.service.IArticleTagService;
import com.personblog.article.service.ICategoryService;
import com.personblog.article.service.IColumnArticleService;
import com.personblog.article.vo.*;
import com.personblog.common.dto.Interaction.BrowseHistoryMessageDTO;
import com.personblog.common.dto.Interaction.CollectionMessageDTO;
import com.personblog.common.dto.Interaction.LikeMessageDTO;
import com.personblog.common.dto.Moderate.AiModerateMessage;
import com.personblog.common.dto.Search.SearchSyncMessageDTO;
import com.personblog.common.dto.Tag.TagDTO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.dto.User.UserLikeMessageDTO;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.MultiLevelCacheUtil;
import com.personblog.common.utils.UserContextHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.personblog.ai.config.mqConfig.AiMqConfig.AI_EXCHANGE;
import static com.personblog.ai.config.mqConfig.AiMqConfig.AI_MODERATE_KEY;
import static com.personblog.article.config.mqConfig.ArticleStatsMqConfig.ARTICLE_STATS_EXCHANGE;
import static com.personblog.article.config.mqConfig.ArticleStatsMqConfig.ARTICLE_STATS_KEY;
import static com.personblog.common.constant.PageConstant.*;
import static com.personblog.common.constant.RedisKeys.*;
import static com.personblog.common.constant.StatusConstant.APPROVED;
import static com.personblog.common.constant.StatusConstant.PENDING;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.common.enums.BizCodeEnum.*;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.INTERACTION_EXCHANGE;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.USER_LIKE_KEY;
import static com.personblog.search.config.mqConfig.SearchMqConfig.*;


/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements IArticleService, ArticleInfoAPI {
    private final ICategoryService categoryService;
    private final IArticleTagService articleTagService;
    private final TagApi tagApi;
    private final UseApi useApi;
    private final FollowApi followApi;
    private final LikeApi likeApi;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final WritingTaskApi writingTaskApi;
    private final AiArticleDraftApi draftApi;
    private final BrowseHistoryApi browseHistoryApi;
    private final IColumnArticleService columnArticleService;
    private final ArticleMapper articleMapper;
    // ========== 新增：缓存相关组件 ==========
    private final MultiLevelCacheUtil cacheUtil;

    // 本地缓存 - Banner
    private Cache<String, List<BannerVO>> bannerCache;

    // 本地缓存 - 热门文章
    private Cache<String, List<HotArticleVO>> hotArticleCache;

    // 本地缓存 - 文章分页列表
    private Cache<String, Page<ArticleListVO>> articlePageCache;

    @PostConstruct
    public void initCaches() {
        // Banner 本地缓存
        bannerCache = Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build();

        // 热门文章本地缓存
        hotArticleCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();

        // 文章分页列表本地缓存
        articlePageCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats()
                .build();
    }
    @Resource(name = "ArticleCountExecutor")
    private Executor articleCountExecutor;
    /**
     * 获取Banner轮播图
     * 缓存策略：Caffeine 本地缓存，1小时
     */
    @Override
    public List<BannerVO> getBanners(Integer size) {
        int limit = (size == null || size <= 0) ? DEFAULT_BANNER_SIZE : Math.min(size, MAX_BANNER_SIZE);
        String cacheKey = ARTICLE_BANNERS + limit;

        // 先查本地缓存
        List<BannerVO> cached = bannerCache.getIfPresent(cacheKey);
        if (cached != null&& !cached.isEmpty()) {
            log.debug("Banner缓存命中");
            return cached;
        }

        // 查询数据库
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1)
                .eq(Article::getIsDeleted, false)
                .eq(Article::getReview, APPROVED)
                .eq(Article::getIsHot, true)
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getViews);

        List<Article> articles = this.page(new Page<>(1, limit), wrapper).getRecords();
        List<BannerVO> result = articles.stream().map(article -> {
            BannerVO vo = new BannerVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCover(article.getCover());
            vo.setArticleId(article.getId());
            return vo;
        }).collect(Collectors.toList());

        // 写入本地缓存
        bannerCache.put(cacheKey, result);
        log.debug("Banner数据已缓存: {}", result.size());

        return result;
    }


    @Override
    public Page<ArticleListVO> getArticlePage(ArticleQueryDTO queryDTO) {
        int current = (queryDTO.getCurrent() == null || queryDTO.getCurrent() <= 0) ? 1 : queryDTO.getCurrent();
        int size = (queryDTO.getSize() == null || queryDTO.getSize() <= 0) ? DEFAULT_PAGE_SIZE : Math.min(queryDTO.getSize(), MAX_PAGE_SIZE);

        // 关注筛选不走缓存（用户相关）
        if (Boolean.TRUE.equals(queryDTO.getFollow())) {
            return getArticlePageNoCache(queryDTO, current, size);
        }

        // 构建缓存key：包含所有查询参数
        String cacheKey = ARTICLE_PAGE + current + ":" + size
                + ":cat:" + queryDTO.getCategoryId()
                + ":tag:" + queryDTO.getTagId()
                + ":author:" + queryDTO.getAuthorId()
                + ":order:" + queryDTO.getOrderBy();

        // 查本地缓存
        Page<ArticleListVO> cached = articlePageCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 查数据库
        Page<ArticleListVO> resultPage = getArticlePageNoCache(queryDTO, current, size);

        // 写入本地缓存
        articlePageCache.put(cacheKey, resultPage);

        return resultPage;
    }

    /**
     * 查询文章分页（不走缓存）
     */
    private Page<ArticleListVO> getArticlePageNoCache(ArticleQueryDTO queryDTO, int current, int size) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1)
                .eq(Article::getIsDeleted, false)
                .eq(Article::getReview, APPROVED);

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Article::getCategoryId, queryDTO.getCategoryId());
        }
        if (queryDTO.getTagId() != null) {
            List<Long> articleIds = articleTagService.lambdaQuery()
                    .eq(ArticleTag::getTagId, queryDTO.getTagId())
                    .list()
                    .stream()
                    .map(ArticleTag::getArticleId)
                    .collect(Collectors.toList());
            if (articleIds.isEmpty()) {
                return new Page<>(current, size);
            }
            wrapper.in(Article::getId, articleIds);
        }
        if (queryDTO.getAuthorId() != null) {
            wrapper.eq(Article::getAuthorId, queryDTO.getAuthorId());
        }
        
        // 关注筛选
        if (Boolean.TRUE.equals(queryDTO.getFollow())) {
            Long currentUserId = UserContextHolder.getUserId();
            if (currentUserId == null) {
                return new Page<>(current, size);
            }
            List<Long> followingIds = followApi.getFollowingIds(currentUserId);
            if (followingIds.isEmpty()) {
                return new Page<>(current, size);
            }
            wrapper.in(Article::getAuthorId, followingIds);
        }

        String orderBy = StringUtils.hasText(queryDTO.getOrderBy()) ? queryDTO.getOrderBy() : "created_at";

        wrapper.orderByDesc(Article::getIsTop);
        applyOrderBy(wrapper, orderBy);
        Page<Article> articlePage = page(new Page<>(current, size), wrapper);

        Page<ArticleListVO> resultPage = new Page<>(current, size, articlePage.getTotal());

        List<ArticleListVO> voList = convertToVOList(articlePage.getRecords());
        resultPage.setRecords(voList);

        return resultPage;
    }

    @Override
    public List<ArticleListVO> getRandomArticles(Integer size) {
        int limit = (size == null || size <= 0) ? DEFAULT_RANDOM_SIZE : Math.min(size, MAX_RANDOM_SIZE);

        List<Article> articles = baseMapper.selectRandomArticles(limit);

        return convertToVOList(articles);
    }

    /**
     * 获取热门文章
     * 查询由定时任务 refreshHotArticles 预计算的 is_hot 标记
     * 缓存策略：Caffeine 本地缓存，5分钟
     */
    @Override
    public List<HotArticleVO> getHotArticles(Integer size) {
        int limit = (size == null || size <= 0) ? DEFAULT_HOT_SIZE : Math.min(size, MAX_HOT_SIZE);
        String cacheKey = ARTICLE_HOT + limit;

        // 先查本地缓存
        List<HotArticleVO> cached = hotArticleCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("热门文章缓存命中");
            return cached;
        }

        // 查询 is_hot=true 的文章，按综合热度分降序
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getIsHot, true)
                .eq(Article::getStatus, 1)
                .eq(Article::getIsDeleted, false)
                .eq(Article::getReview, APPROVED)
                .orderByDesc(Article::getViews, Article::getLikes, Article::getComments, Article::getCollections);
        List<Article> articles = page(new Page<>(1, limit), wrapper).getRecords();

        List<HotArticleVO> result = articles.stream().map(article -> {
                    HotArticleVO vo = new HotArticleVO();
                    vo.setId(article.getId());
                    vo.setTitle(article.getTitle());
                    // 实时浏览量 = DB基础值 + Redis未刷新增量
                    long view = article.getViews() != null ? article.getViews() : 0L;
                    Object browseCount = redisTemplate.opsForHash().get(BROWSE_COUNT_KEY, article.getId().toString());
                    if (browseCount != null) {
                        view += Long.parseLong(browseCount.toString());
                    }
                    vo.setViews(view);
                    return vo;
                }).collect(Collectors.toList());

        // 写入本地缓存
        hotArticleCache.put(cacheKey, result);
        log.debug("热门文章已缓存: {}", result.size());

        return result;
    }
    /**
     * 获取文章详情
     * 缓存策略：多级缓存（本地缓存 + Redis），10分钟
     * 变更时清除缓存
     * 浏览量实时性保证：
     * - 缓存中存储数据库的基础浏览量
     * - Redis BROWSE_COUNT_KEY 存储浏览量增量
     * - 实时浏览量 = 缓存基础值 + Redis增量
     * - 定时任务将增量同步到数据库后清除 Redis 增量
     * 点赞量实时性保证：
     * - 点赞量完全由 Redis 管理（likeApi）
     * - 每次从 Redis 获取最新点赞数
     * 重要：必须创建新对象副本再修改，避免污染缓存中的原始对象
     */
    @Override
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

    @Override
    public ArticleInteractionVO getArticleInteraction(Long id) {
        Long userId = UserContextHolder.getUserId() == null ? -1 : UserContextHolder.getUserId();
        
        // Pipeline 批量查询实时互动状态（isCollected、isLiked、浏览量增量、点赞数）
        InteractionQueryResult interaction = queryArticleInteractions(id, userId);

        Article article = getById(id);
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

    /**
     * 文章互动数据查询结果（Pipeline 批量返回）
     */
    private record InteractionQueryResult(
            boolean isCollected,
            boolean isLiked,
            Long browseCount,
            long likeCount
    ) {}

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
        boolean isCollected = loggedIn ? (Boolean) results.get(0) : false;
        boolean isLiked = loggedIn ? likeApi.isLiked(articleId, userId, ARTICLE) : false;
        String browseCountStr = (String) results.get(offset);
        Long browseCount = browseCountStr != null ? Long.parseLong(browseCountStr) : null;
        long likeCount = (Long) results.get(offset + 1);

        return new InteractionQueryResult(isCollected, isLiked, browseCount, likeCount);
    }

    /**
     * 从数据库加载文章基础信息（含正文、分类、标签）
     * ArticleMetadataVO 包含 content 字段，统一缓存，避免分拆加载的网络开销
     */
    private ArticleMetadataVO loadArticleMetadataFromDB(Long id) {
        Article article = getById(id);
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
        Set<Long> tagIds = getTagIdsByArticleId(article.getId());
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

    @Override
    public List<RelatedArticleVO> getRelatedArticles(Long articleId, Integer limit) {
        int size = (limit == null || limit <= 0) ? 3 : Math.min(limit, 10);

        Article currentArticle = this.getById(articleId);
        if (currentArticle == null) {
            return Collections.emptyList();
        }

        Set<Long> tagIds = getTagIdsByArticleId(articleId);

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
            List<Long> sameCategoryIds = page(new Page<>(1, remainSize), categoryWrapper).getRecords()
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
        List<Article> articles = page(new Page<>(1, size), resultWrapper).getRecords();

        return articles.stream().map(article -> {
            RelatedArticleVO vo = new RelatedArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setCover(article.getCover());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<MyArticleVO> getMyArticles(Long userId, Integer current, Integer size, Integer status, String orderBy) {
        int pageNum = (current == null || current <= 0) ? 1 : current;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 50);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getAuthorId, userId)
                .eq(Article::getIsDeleted, false);

        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }

        applyOrderBy(wrapper, orderBy);

        Page<Article> articlePage = this.page(new Page<>(pageNum, pageSize), wrapper);

        Page<MyArticleVO> resultPage = new Page<>(pageNum, pageSize, articlePage.getTotal());

        List<MyArticleVO> voList = articlePage.getRecords().stream().map(article -> MyArticleVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .cover(article.getCover())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .status(article.getStatus())
                .review(article.getReview())
                .views(article.getViews())
                .likes(article.getLikes())
                .comments(article.getComments())
                .build()).collect(Collectors.toList());

        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public ArticleEditVO getEditArticle(Long userId, Long articleId) {
        Article article = getAndValidateArticle(articleId, userId);

        ArticleEditVO vo = new ArticleEditVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setCover(article.getCover());
        vo.setCategoryId(article.getCategoryId());
        vo.setStatus(article.getStatus());

        vo.setTagIds(new ArrayList<>(getTagIdsByArticleId(articleId)));
        return vo;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticlePublishVO createArticle(Long userId, ArticlePublishDTO dto) {
        validatePublishDTO(dto);
        validateCategory(dto.getCategoryId());
        Set<Long> tagIds = resolveTagIds(dto.getTagIds(), dto.getTagNames());

        LocalDateTime now = LocalDateTime.now();
        Article article = new Article();
        article.setTitle(dto.getTitle().trim());
        article.setSummary(StrUtil.trim(dto.getSummary()));
        article.setContent(StrUtil.trim(dto.getContent()));
        article.setCover(StrUtil.trim(dto.getCover()));
        article.setAuthorId(userId);
        article.setCategoryId(dto.getCategoryId());
        article.setViews(0L);
        article.setLikes(0L);
        article.setComments(0L);
        article.setCollections(0L);
        article.setIsTop(false);
        article.setIsHot(false);
        article.setStatus(dto.getStatus());
        article.setIsDeleted(false);
        article.setReview(PENDING);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        boolean save = save(article);
        Long articleId = article.getId();
        Integer status = dto.getStatus();

        saveArticleTags(articleId, tagIds);

        if (save && status == 1) {
            // 异步发送 MQ 消息更新统计（用户文章数、标签次数、分类文章数、标签缓存）
            rabbitTemplate.convertAndSend(ARTICLE_STATS_EXCHANGE, ARTICLE_STATS_KEY,
                    ArticleStatsMessage.builder()
                            .articleId(articleId)
                            .tagIds(tagIds)
                            .categoryId(dto.getCategoryId())
                            .userId(userId)
                            .delta(1)
                            .build());
            //更新es索引
            sendSearchSyncMessage(OPERATION_SYNC, articleId);
            removeArticleCache(articleId,"创建文章后删除缓存");
            sendCreateMessage(articleId, article);
        }

        // 关联写作任务（AI写作时传入taskId）
        if (dto.getTaskId() != null) {
            // 根据状态设置最终动作：1=发布，0=草稿
            String finalAction = (status == 1) ? "publish" : "draft";
            writingTaskApi.completeTask(dto.getTaskId(), articleId, finalAction);
            draftApi.deleteByTaskId(dto.getTaskId());

        }

        ArticlePublishVO vo = new ArticlePublishVO();
        vo.setId(articleId);
        vo.setStatus(status);
        return vo;
    }

    private void sendCreateMessage(Long articleId, Article article) {
        AiModerateMessage moderateMessage = AiModerateMessage.builder()
                .bizId(articleId)
                .bizType(ARTICLE)
                .content(article.getContent())
                .authorId(article.getAuthorId())
                .title(article.getTitle())
                .build();
        rabbitTemplate.convertAndSend(AI_EXCHANGE, AI_MODERATE_KEY, moderateMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticlePublishVO updateArticle(Long userId, Long articleId, ArticlePublishDTO dto) {
        validatePublishDTO(dto);
        validateCategory(dto.getCategoryId());
        Set<Long> tagIds = resolveTagIds(dto.getTagIds(), dto.getTagNames());

        Article oldArticle = getAndValidateArticle(articleId, userId);

        Article article = new Article();
        article.setId(articleId);
        article.setTitle(dto.getTitle().trim());
        article.setSummary(StrUtil.trim(dto.getSummary()));
        article.setContent(StrUtil.trim(dto.getContent()));
        article.setAuthorId(userId);
        article.setCover(StrUtil.trim(dto.getCover()));
        article.setReview(PENDING);
        article.setCategoryId(dto.getCategoryId());
        article.setStatus(dto.getStatus());
        article.setUpdatedAt(LocalDateTime.now());
        boolean b = updateById(article);
        if(b){
            // 同步处理标签关联变更（必须在事务内）
            Set<Long> oldTagIds = articleTagService.lambdaQuery()
                    .eq(ArticleTag::getArticleId, articleId)
                    .list()
                    .stream()
                    .map(ArticleTag::getTagId)
                    .collect(Collectors.toSet());
            articleTagService.lambdaUpdate().eq(ArticleTag::getArticleId, articleId).remove();
            saveArticleTags(articleId, tagIds);

            // 计算发布状态变更偏移量
            int delta = 0;
            if ((article.getStatus() == 1 && oldArticle.getStatus() == 0)) {
                delta = 1;
                sendCreateMessage(articleId, article);
            } else if (article.getStatus() == 0 && oldArticle.getStatus() == 1) {
                delta = -1;
            }

            // 异步发送 MQ 消息更新统计（标签使用次数、用户文章数、分类文章数、标签缓存）
            rabbitTemplate.convertAndSend(ARTICLE_STATS_EXCHANGE, ARTICLE_STATS_KEY,
                    ArticleStatsMessage.builder()
                            .articleId(articleId)
                            .oldTagIds(oldTagIds)
                            .tagIds(tagIds)
                            .categoryId(article.getCategoryId())
                            .userId(userId)
                            .delta(delta)
                            .build());

            // 同步搜索引擎
            sendSearchSyncMessage(OPERATION_SYNC, articleId);
            // 清除相关缓存
            removeArticleCache(articleId, "更新文章后清除缓存: articleId={}");
        }
        ArticlePublishVO vo = new ArticlePublishVO();
        vo.setId(articleId);
        vo.setStatus(dto.getStatus());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long userId, Long articleId) {
        Article article = getAndValidateArticle(articleId, userId);
        Set<Long> tagIds = getTagIdsByArticleId(articleId);
        boolean removeById = removeById(articleId);
        if (removeById&&article.getStatus()==1){
            // 异步发送 MQ 消息更新统计（用户文章数、标签次数、分类文章数）
            rabbitTemplate.convertAndSend(ARTICLE_STATS_EXCHANGE, ARTICLE_STATS_KEY,
                    ArticleStatsMessage.builder()
                            .articleId(articleId)
                            .tagIds(tagIds)
                            .categoryId(article.getCategoryId())
                            .userId(userId)
                            .delta(-1)
                            .build());
            sendSearchSyncMessage(OPERATION_DELETE, articleId);
            CompletableFuture.runAsync(()->columnArticleService.removeArticleFromAllColumns(articleId),articleCountExecutor);
            removeArticleCache(articleId, "删除文章后清除缓存: articleId={}");
        }
    }

    private void removeArticleCache(Long articleId, String s) {
        // 清除相关缓存
        hotArticleCache.invalidateAll();
        articlePageCache.invalidateAll();
        cacheUtil.evict(ARTICLE_DETAIL + articleId);
        log.info(s, articleId);
    }

    // ========== 以下为提取的公共辅助方法 ==========

    /**
     * 根据文章ID查询关联的标签ID集合
     * @param articleId 文章ID
     * @return 标签ID集合
     */
    private Set<Long> getTagIdsByArticleId(Long articleId) {
        return articleTagService.lambdaQuery()
                .eq(ArticleTag::getArticleId, articleId)
                .select(ArticleTag::getTagId)
                .list().stream()
                .map(ArticleTag::getTagId)
                .collect(Collectors.toSet());
    }

    /**
     * 校验文章存在性及作者权限，返回文章实体
     * @param articleId 文章ID
     * @param userId 当前用户ID
     * @return 校验通过的文章实体
     */
    private Article getAndValidateArticle(Long articleId, Long userId) {
        Article article = getById(articleId);
        if (article == null || Boolean.TRUE.equals(article.getIsDeleted())) {
            throw new BizException(NOT_ARTICLE);
        }
        if (!Objects.equals(article.getAuthorId(), userId)) {
            throw new BizException(NO_POWER);
        }
        return article;
    }

    /**
     * 根据排序字段为wrapper添加排序条件
     * @param wrapper 查询条件构造器
     * @param orderBy 排序字段（views/likes/created_at）
     */
    private void applyOrderBy(LambdaQueryWrapper<Article> wrapper, String orderBy) {
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
     * 批量清除文章详情的多级缓存
     * @param articleIds 文章ID列表
     */
    private void evictArticleDetailCaches(List<Long> articleIds) {
        for (Long articleId : articleIds) {
            cacheUtil.evict(ARTICLE_DETAIL + articleId);
        }
    }

    /**
     * 将文章实体列表转换为VO列表
     * @param articles 文章实体列表
     * @return VO列表
     */
    private List<ArticleListVO> convertToVOList(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = articles.stream()
                .map(Article::getAuthorId)
                .collect(Collectors.toSet());

        Set<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserDTO> userMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            List<UserDTO> users = useApi.getUserInfo(authorIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        Map<Long, Category> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryService.listByIds(categoryIds);
            categoryMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));
        }

        List<Long> articleIds = articles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        Map<Long, List<String>> articleTagMap = getArticleTagNames(articleIds);

        // 批量从Redis获取浏览量增量
        List<String> articleIdStr = articleIds.stream()
                .map(String::valueOf)
                .toList();
        List<Object> browseCounts = redisTemplate.opsForHash().multiGet(BROWSE_COUNT_KEY, new ArrayList<>(articleIdStr));
        
        // 构建文章ID与浏览量增量的映射
        Map<Long, Long> browseCountMap = new HashMap<>();
        for (int i = 0; i < articleIds.size(); i++) {
            Object count = browseCounts.get(i);
            if (count != null) {
                browseCountMap.put(articleIds.get(i), Long.parseLong(count.toString()));
            }
        }

        Map<Long, UserDTO> finalUserMap = userMap;
        Map<Long, Category> finalCategoryMap = categoryMap;
        Map<Long, Long> likesTime = likeApi.getLikesTime(articleIds, ARTICLE);
        return articles.stream().map(article -> {
            ArticleListVO vo = new ArticleListVO();
            BeanUtils.copyProperties(article, vo,"likes");
            vo.setLikes(likesTime.get(article.getId()));
            // 将数据库浏览量与Redis增量相加
            Long browseIncrement = browseCountMap.get(article.getId());
            if (browseIncrement != null) {
                vo.setViews(article.getViews() + browseIncrement);
            }

            UserDTO author = finalUserMap.get(article.getAuthorId());
            if (author != null) {
                ArticleListVO.AuthorVO authorVO = new ArticleListVO.AuthorVO();
                authorVO.setId(author.getId());
                authorVO.setName(author.getNickname());
                authorVO.setAvatar(author.getAvatar());
                vo.setAuthor(authorVO);
            }

            Category category = finalCategoryMap.get(article.getCategoryId());
            if (category != null) {
                vo.setCategory(category.getName());
            }

            vo.setTags(articleTagMap.getOrDefault(article.getId(), Collections.emptyList()));

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取文章的标签名称映射
     * @param articleIds 文章ID列表
     * @return 文章ID -> 标签名称列表的映射
     */
    private Map<Long, List<String>> getArticleTagNames(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ArticleTag> articleTags = articleTagService.lambdaQuery()
                .in(ArticleTag::getArticleId, articleIds)
                .list();

        if (articleTags.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> tagIds = articleTags.stream()
                .map(ArticleTag::getTagId)
                .collect(Collectors.toSet());

        List<TagVO> tagVOs = tagApi.getTagsByIds(tagIds);
        Map<Long, String> tagNameMap = tagVOs.stream()
                .collect(Collectors.toMap(TagVO::getId, TagVO::getName));

        Map<Long, List<String>> result = new HashMap<>();
        articleTags.forEach(at -> {
            Long articleId = at.getArticleId();
            String tagName = tagNameMap.get(at.getTagId());
            if (tagName != null) {
                result.computeIfAbsent(articleId, k -> new ArrayList<>()).add(tagName);
            }
        });

        return result;
    }

    private void validatePublishDTO(ArticlePublishDTO dto) {
        if (dto == null) {
            throw new BizException(PARAM_EMPTY);
        }
        String title = StrUtil.trim(dto.getTitle());
        if (StrUtil.isBlank(title)) {
            throw new BizException(ARTICLE_TITLE_EMPTY);
        }
        if (title.length() > 200) {
            throw new BizException(ARTICLE_TITLE_TOO_LONG);
        }
        String summary = StrUtil.trim(dto.getSummary());
        if (StrUtil.isNotBlank(summary) && summary.length() > 500) {
            throw new BizException(ARTICLE_SUMMARY_TOO_LONG);
        }
        List<String> tagNames = dto.getTagNames();
        if (CollectionUtil.isNotEmpty(tagNames)) {
            for (String tagName : tagNames) {
                String value = StrUtil.trim(tagName);
                if (StrUtil.isBlank(value)) {
                    throw new BizException(TAG_NAME_EMPTY);
                }
                if (value.length() > 20) {
                    throw new BizException(TAG_NAME_TOO_LONG);
                }
            }
        }
        if (!Objects.equals(dto.getStatus(), 0) && !Objects.equals(dto.getStatus(), 1)) {
            throw new BizException(ARTICLE_STATUS_INVALID);
        }
        String content = StrUtil.trim(dto.getContent());
        if (Objects.equals(dto.getStatus(), 1) && StrUtil.isBlank(content)) {
            throw new BizException(ARTICLE_CONTENT_EMPTY);
        }
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        boolean exists = categoryService.lambdaQuery()
                .eq(Category::getId, categoryId)
                .exists();
        if (!exists) {
            throw new BizException(CATEGORY_NOT_EXIST);
        }
    }

    private Set<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        Set<Long> resultIds = new HashSet<>();
        if (CollectionUtil.isNotEmpty(tagIds)) {
            Set<Long> uniqueTagIds = tagIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(uniqueTagIds)) {
                long count = tagApi.countExistingTags(uniqueTagIds);
                if (count != uniqueTagIds.size()) {
                    throw new BizException(TAG_NOT_EXIST);
                }
                resultIds.addAll(uniqueTagIds);
            }
        }

        if (CollectionUtil.isNotEmpty(tagNames)) {
            Set<String> normalizedNames = tagNames.stream()
                    .map(StrUtil::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (CollectionUtil.isNotEmpty(normalizedNames)) {
                List<TagDTO> existingTags = tagApi.getTagsByNames(normalizedNames);
                Map<String, TagDTO> nameTagMap = existingTags.stream()
                        .collect(Collectors.toMap(TagDTO::getName, tag -> tag, (a, b) -> a));
                resultIds.addAll(existingTags.stream().map(TagDTO::getId).toList());

                List<TagDTO> newTags = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();
                for (String tagName : normalizedNames) {
                    if (!nameTagMap.containsKey(tagName)) {
                        TagDTO tag = new TagDTO();
                        tag.setName(tagName);
                        tag.setUseCount(0L);
                        tag.setCreatedAt(now);
                        newTags.add(tag);
                    }
                }
                if (CollectionUtil.isNotEmpty(newTags)) {
                    tagApi.saveTags(newTags);
                    resultIds.addAll(newTags.stream().map(TagDTO::getId).toList());
                }
            }
        }

        if (resultIds.size() > 10) {
            throw new BizException(TAG_COUNT_EXCEED);
        }
        return resultIds;
    }

    private void saveArticleTags(Long articleId, Set<Long> tagIds) {
        if (CollectionUtil.isEmpty(tagIds)) {
            return;
        }
        List<ArticleTag> relationList = tagIds.stream().map(tagId -> {
            ArticleTag relation = new ArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tagId);
            relation.setCreatedAt(LocalDateTime.now());
            return relation;
        }).collect(Collectors.toList());
        articleTagService.saveBatch(relationList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLikeCount(List<LikeMessageDTO> dtoList) {
        List<Long> articleIds = dtoList.stream()
                .map(LikeMessageDTO::getId)
                .collect(Collectors.toList());
        
        Map<Long, Article> oldArticleMap = listByIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, article -> article));
        
        List<Article> list = new ArrayList<>(dtoList.size());
        List<UserLikeMessageDTO> userLikeMessages = new ArrayList<>();
        
        for (LikeMessageDTO dto : dtoList) {
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
        
        updateBatchById(list);
        
        if (!userLikeMessages.isEmpty()) {
            rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, USER_LIKE_KEY, userLikeMessages);
        }

        // 清理文章详情缓存
        evictArticleDetailCaches(articleIds);
    }

    @Override
    public void updateCollectionCount(CollectionMessageDTO dto) {
        Article article = new Article();
        article.setId(dto.getArticleId());
        article.setCollections(dto.getCollectionTimes());
        this.updateById(article);
        // 清理文章详情缓存
        cacheUtil.evict(ARTICLE_DETAIL + dto.getArticleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBrowseCount(List<BrowseHistoryMessageDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }
        
        // 获取所有文章ID
        List<Long> articleIds = dtoList.stream()
                .map(BrowseHistoryMessageDTO::getArticleId)
                .collect(Collectors.toList());
        
        // 批量查询数据库中的文章
        List<Article> dbArticles = listByIds(articleIds);
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
        updateBatchById(articles);

        // 清理文章详情缓存
        evictArticleDetailCaches(articleIds);
    }
    @Override
    @Async("CommentExecutor")
    public void updateCommentCount(Long articleId, int dealt) {
        lambdaUpdate()
                .eq(Article::getId,articleId)
                .setSql("comments = comments + {0}", dealt)
                .update();
    }

    @Override
    public Long getArticleAuthorId(Long articleId) {
        return getById(articleId).getAuthorId();
    }

    @Override
    public void updateArticleReviewStatus(Long articleId, String status) {
        boolean update = lambdaUpdate()
                .eq(Article::getId, articleId)
                .eq(Article::getReview, PENDING)
                .set(Article::getReview, status)
                .update();
        if (update) {
            log.info("文章审核状态更新成功: articleId={}, status={}", articleId, status);
            // 清除文章详情缓存
            cacheUtil.evict(ARTICLE_DETAIL + articleId);
        } else {
            log.warn("文章审核状态更新失败，文章不存在或已审核: articleId={}", articleId);
        }
    }

    @Override
    public String getArticleTitle(Long articleId) {
        Article article = getById(articleId);
        return article != null ? article.getTitle() : null;
    }

    /**
     * 刷新热门文章标记
     * 1. 清除所有 is_hot 标记
     * 2. 根据综合热度分重新标记 Top N
     * 3. 清除本地缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshHotArticles() {
        articleMapper.clearAllHotFlags();
        articleMapper.refreshHotFlags(MAX_HOT_SIZE);
        hotArticleCache.invalidateAll();
        log.info("热门文章标记已刷新, Top {}", MAX_HOT_SIZE);
    }
    // ==================== MQ 消息发送 ====================

    /**
     * 发送搜索同步消息到 MQ
     *
     * @param operation  操作类型：sync-同步，delete-删除
     * @param articleId  文章ID
     */
    private void sendSearchSyncMessage(String operation, Long articleId) {
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

    // ==================== 管理端接口实现 ====================

    @Override
    public AdminDashboardVO getDashboardStatistics() {
        // 获取文章总数
        long articlesTotal = this.count();

        // 获取用户总数（通过 UseApi）
        List<UserDTO> allUsers = useApi.getUserInfo(Collections.emptyList());
        long usersTotal = allUsers != null ? allUsers.size() : 0;

        // 获取评论总数和问答总数
        // 注意：这里需要通过其他服务获取，暂时返回0
        // 实际实现需要注入 ICommentService 和 IQuestionService
        long commentsTotal = 0;
        long questionsTotal = 0;

        return AdminDashboardVO.builder()
                .articlesTotal(articlesTotal)
                .articlesChange("+0%")
                .usersTotal(usersTotal)
                .usersChange("+0%")
                .commentsTotal(commentsTotal)
                .commentsChange("+0%")
                .questionsTotal(questionsTotal)
                .questionsChange("+0%")
                .build();
    }

    @Override
    public AdminTrendsVO getTrendsData(Integer year) {
        int targetYear = (year != null) ? year : LocalDateTime.now().getYear();

        // 月份标签
        List<String> months = Arrays.asList("1月", "2月", "3月", "4月", "5月", "6月",
                "7月", "8月", "9月", "10月", "11月", "12月");

        // 查询每月文章新增数
        List<Long> articles = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            LocalDateTime start = LocalDateTime.of(targetYear, month, 1, 0, 0, 0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            long count = this.lambdaQuery()
                    .ge(Article::getCreatedAt, start)
                    .le(Article::getCreatedAt, end)
                    .count();
            articles.add(count);
        }

        // 用户、评论、问答的月度数据需要其他服务支持，暂时返回模拟数据
        List<Long> users = Arrays.asList(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        List<Long> comments = Arrays.asList(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        List<Long> questions = Arrays.asList(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);

        return AdminTrendsVO.builder()
                .months(months)
                .articles(articles)
                .users(users)
                .comments(comments)
                .questions(questions)
                .build();
    }

    @Override
    public List<AdminRecentArticleVO> getRecentArticles(Integer size) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getIsDeleted, false)
                .orderByDesc(Article::getCreatedAt)
                .last("LIMIT " + size);

        List<Article> articles = this.list(wrapper);

        return articles.stream()
                .map(article -> AdminRecentArticleVO.builder()
                        .id(article.getId())
                        .title(article.getTitle())
                        .status(article.getStatus() == 1 ? "published" : "draft")
                        .views(article.getViews())
                        .createdAt(article.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Page<AdminArticleVO> getAdminArticlePage(AdminArticleQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getIsDeleted, false);

        // 关键词搜索
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(Article::getTitle, dto.getKeyword());
        }

        // 状态筛选
        if (StrUtil.isNotBlank(dto.getStatus()) && !"all".equals(dto.getStatus())) {
            switch (dto.getStatus()) {
                case "published":
                    wrapper.eq(Article::getStatus, 1);
                    break;
                case "draft":
                    wrapper.eq(Article::getStatus, 0);
                    break;
                case "offline":
                    wrapper.eq(Article::getStatus, 2);
                    break;
            }
        }

        // 审核状态筛选
        if (StrUtil.isNotBlank(dto.getReviewStatus()) && !"all".equals(dto.getReviewStatus())) {
            wrapper.eq(Article::getReview, dto.getReviewStatus());
        }

        // 分类筛选
        if (dto.getCategoryId() != null) {
            wrapper.eq(Article::getCategoryId, dto.getCategoryId());
        }

        // 排序
        String sortField = StrUtil.isNotBlank(dto.getSortField()) ? dto.getSortField() : "created_at";
        boolean isAsc = "asc".equalsIgnoreCase(dto.getSortOrder());

        switch (sortField) {
            case "views":
                wrapper.orderBy(true, isAsc, Article::getViews);
                break;
            case "likes":
                wrapper.orderBy(true, isAsc, Article::getLikes);
                break;
            default:
                wrapper.orderBy(true, isAsc, Article::getCreatedAt);
                break;
        }

        Page<Article> articlePage = this.page(new Page<>(current, size), wrapper);
        Page<AdminArticleVO> resultPage = new Page<>(current, size, articlePage.getTotal());

        // 批量获取作者信息
        Set<Long> authorIds = articlePage.getRecords().stream()
                .map(Article::getAuthorId)
                .collect(Collectors.toSet());
        Map<Long, UserDTO> userMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            List<UserDTO> users = useApi.getUserInfo(authorIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 批量获取分类信息
        Set<Long> categoryIds = articlePage.getRecords().stream()
                .map(Article::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryService.listByIds(categoryIds);
            categoryMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));
        }

        // 批量获取标签信息
        List<Long> articleIds = articlePage.getRecords().stream()
                .map(Article::getId)
                .collect(Collectors.toList());
        Map<Long, List<TagVO>> articleTagMap = getArticleTagVOs(articleIds);

        Map<Long, UserDTO> finalUserMap = userMap;
        Map<Long, Category> finalCategoryMap = categoryMap;

        List<AdminArticleVO> voList = articlePage.getRecords().stream()
                .map(article -> {
                    AdminArticleVO vo = AdminArticleVO.builder()
                            .id(article.getId())
                            .title(article.getTitle())
                            .authorId(article.getAuthorId())
                            .categoryId(article.getCategoryId())
                            .status(article.getStatus() == 1 ? "published" : (article.getStatus() == 0 ? "draft" : "offline"))
                            .reviewStatus(article.getReview())
                            .isTop(article.getIsTop())
                            .views(article.getViews())
                            .likes(article.getLikes())
                            .comments(article.getComments())
                            .collections(article.getCollections())
                            .cover(article.getCover())
                            .summary(article.getSummary())
                            .createdAt(article.getCreatedAt())
                            .updatedAt(article.getUpdatedAt())
                            .build();

                    // 设置作者信息
                    UserDTO author = finalUserMap.get(article.getAuthorId());
                    if (author != null) {
                        vo.setAuthorName(author.getNickname());
                        vo.setAuthorAvatar(author.getAvatar());
                    }

                    // 设置分类信息
                    Category category = finalCategoryMap.get(article.getCategoryId());
                    if (category != null) {
                        vo.setCategoryName(category.getName());
                    }

                    // 设置标签信息
                    vo.setTags(articleTagMap.getOrDefault(article.getId(), Collections.emptyList()));

                    return vo;
                })
                .collect(Collectors.toList());

        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 获取文章的标签VO映射
     */
    private Map<Long, List<TagVO>> getArticleTagVOs(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ArticleTag> articleTags = articleTagService.lambdaQuery()
                .in(ArticleTag::getArticleId, articleIds)
                .list();

        if (articleTags.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> tagIds = articleTags.stream()
                .map(ArticleTag::getTagId)
                .collect(Collectors.toSet());

        List<TagVO> tagVOs = tagApi.getTagsByIds(tagIds);
        Map<Long, TagVO> tagVOMap = tagVOs.stream()
                .collect(Collectors.toMap(TagVO::getId, tag -> tag));

        Map<Long, List<TagVO>> result = new HashMap<>();
        articleTags.forEach(at -> {
            Long articleId = at.getArticleId();
            TagVO tagVO = tagVOMap.get(at.getTagId());
            if (tagVO != null) {
                result.computeIfAbsent(articleId, k -> new ArrayList<>()).add(tagVO);
            }
        });

        return result;
    }

    @Override
    public AdminArticleVO getAdminArticleDetail(Long id) {
        Article article = this.getById(id);
        if (article == null || Boolean.TRUE.equals(article.getIsDeleted())) {
            throw new BizException(NOT_ARTICLE);
        }

        AdminArticleVO vo = AdminArticleVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .authorId(article.getAuthorId())
                .categoryId(article.getCategoryId())
                .status(article.getStatus() == 1 ? "published" : (article.getStatus() == 0 ? "draft" : "offline"))
                .reviewStatus(article.getReview())
                .isTop(article.getIsTop())
                .views(article.getViews())
                .likes(article.getLikes())
                .comments(article.getComments())
                .collections(article.getCollections())
                .cover(article.getCover())
                .summary(article.getSummary())
                .content(article.getContent())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();

        // 获取作者信息
        List<UserDTO> users = useApi.getUserInfo(Collections.singletonList(article.getAuthorId()));
        if (users != null && !users.isEmpty()) {
            vo.setAuthorName(users.getFirst().getNickname());
            vo.setAuthorAvatar(users.getFirst().getAvatar());
        }

        // 获取分类信息
        if (article.getCategoryId() != null) {
            Category category = categoryService.getById(article.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // 获取标签信息
        Set<Long> tagIds = getTagIdsByArticleId(article.getId());
        if (CollectionUtil.isNotEmpty(tagIds)) {
            List<TagVO> tagVOs = tagApi.getTagsByIds(tagIds);
            vo.setTags(tagVOs);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticleByAdmin(Long id) {
        Article article = this.getById(id);
        if (article == null || Boolean.TRUE.equals(article.getIsDeleted())) {
            throw new BizException(NOT_ARTICLE);
        }

        // 逻辑删除
        Article updateArticle = new Article();
        updateArticle.setId(id);
        updateArticle.setIsDeleted(true);
        updateArticle.setUpdatedAt(LocalDateTime.now());
        this.updateById(updateArticle);

        // 如果是已发布文章，更新相关计数
        if (article.getStatus() == 1) {
            Set<Long> tagIds = getTagIdsByArticleId(id);
            // 异步发送 MQ 消息更新统计（用户文章数、标签次数、分类文章数）
            rabbitTemplate.convertAndSend(ARTICLE_STATS_EXCHANGE, ARTICLE_STATS_KEY,
                    ArticleStatsMessage.builder()
                            .articleId(id)
                            .tagIds(tagIds)
                            .categoryId(article.getCategoryId())
                            .userId(article.getAuthorId())
                            .delta(-1)
                            .build());
            sendSearchSyncMessage(OPERATION_DELETE, id);
        }

        removeArticleCache(id, "管理端删除文章后清除缓存: articleId={}");
    }

    @Override
    public void approveArticle(Long id) {
        Article article = this.getById(id);
        if (article == null || Boolean.TRUE.equals(article.getIsDeleted())) {
            throw new BizException(NOT_ARTICLE);
        }

        lambdaUpdate()
                .eq(Article::getId, id)
                .set(Article::getReview, APPROVED)
                .set(Article::getUpdatedAt, LocalDateTime.now())
                .update();

        cacheUtil.evict(ARTICLE_DETAIL + id);
    }

    @Override
    public void rejectArticle(Long id, String reason) {
        Article article = this.getById(id);
        if (article == null || Boolean.TRUE.equals(article.getIsDeleted())) {
            throw new BizException(NOT_ARTICLE);
        }

        lambdaUpdate()
                .eq(Article::getId, id)
                .set(Article::getReview, "rejected")
                .set(Article::getUpdatedAt, LocalDateTime.now())
                .update();

        cacheUtil.evict(ARTICLE_DETAIL + id);
        // TODO: 可以发送通知给作者，告知拒绝原因
    }

    @Override
    public void setArticleTop(Long id, Boolean isTop) {
        Article article = this.getById(id);
        if (article == null || Boolean.TRUE.equals(article.getIsDeleted())) {
            throw new BizException(NOT_ARTICLE);
        }

        lambdaUpdate()
                .eq(Article::getId, id)
                .set(Article::getIsTop, isTop)
                .set(Article::getUpdatedAt, LocalDateTime.now())
                .update();

        // 清除缓存
        hotArticleCache.invalidateAll();
        articlePageCache.invalidateAll();
    }
}
