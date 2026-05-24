package com.personblog.article.BizService;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.adminAPI.TagApi;
import com.personblog.api.adminAPI.TagVO;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.article.dto.article.AdminArticleQueryDTO;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.entity.Category;
import com.personblog.article.service.IArticleService;
import com.personblog.article.service.IArticleTagService;
import com.personblog.article.service.ICategoryService;
import com.personblog.article.vo.Admin.AdminArticleVO;
import com.personblog.article.vo.Admin.AdminDashboardVO;
import com.personblog.article.vo.Admin.AdminRecentArticleVO;
import com.personblog.article.vo.Admin.AdminTrendsVO;
import com.personblog.common.dto.MqMessage.article.ArticleStatsMessage;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.utils.MultiLevelCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.personblog.article.config.cachConfig.ArticleCacheConfig.articlePageCache;
import static com.personblog.article.config.cachConfig.ArticleCacheConfig.hotArticleCache;
import static com.personblog.article.config.mqConfig.ArticleStatsMqConfig.ARTICLE_STATS_EXCHANGE;
import static com.personblog.article.config.mqConfig.ArticleStatsMqConfig.ARTICLE_STATS_KEY;
import static com.personblog.common.constant.RedisKeys.ARTICLE_DETAIL;
import static com.personblog.common.constant.StatusConstant.APPROVED;
import static com.personblog.common.constant.StatusConstant.REJECT;
import static com.personblog.search.config.mqConfig.SearchMqConfig.OPERATION_DELETE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleAdminBizService {

    private final ICategoryService categoryService;
    private final IArticleTagService articleTagService;
    private final TagApi tagApi;
    private final UseApi useApi;
    private final RabbitTemplate rabbitTemplate;
    private final IArticleService articleService;
    private final MultiLevelCacheUtil cacheUtil;
    private final CommonArticleService commonArticleService;


    public AdminDashboardVO getDashboardStatistics() {
        // 获取文章总数
        long articlesTotal = articleService.count();

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
            long count = articleService.lambdaQuery()
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

    public List<AdminRecentArticleVO> getRecentArticles(Integer size) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getIsDeleted, false)
                .orderByDesc(Article::getCreatedAt)
                .last("LIMIT " + size);

        List<Article> articles = articleService.list(wrapper);

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
    public Page<AdminArticleVO> getAdminArticlePage(AdminArticleQueryDTO dto) {
        int[] pageParams = commonArticleService.normalizePageParams(dto.getCurrent(), dto.getSize(), 10, 50);
        int current = pageParams[0];
        int size = pageParams[1];

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

        Page<Article> articlePage = articleService.page(new Page<>(current, size), wrapper);
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
    public AdminArticleVO getAdminArticleDetail(Long id) {
        Article article = commonArticleService.getArticleIfExists(id);

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
        Set<Long> tagIds = commonArticleService.getTagIdsByArticleId(article.getId());
        if (CollectionUtil.isNotEmpty(tagIds)) {
            List<TagVO> tagVOs = tagApi.getTagsByIds(tagIds);
            vo.setTags(tagVOs);
        }

        return vo;
    }
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticleByAdmin(Long id) {
        Article article = commonArticleService.getArticleIfExists(id);

        // 逻辑删除
        Article updateArticle = new Article();
        updateArticle.setId(id);
        updateArticle.setIsDeleted(true);
        updateArticle.setUpdatedAt(LocalDateTime.now());
        articleService.updateById(updateArticle);

        // 如果是已发布文章，更新相关计数
        if (article.getStatus() == 1) {
            Set<Long> tagIds = commonArticleService.getTagIdsByArticleId(id);
            // 异步发送 MQ 消息更新统计（用户文章数、标签次数、分类文章数）
            rabbitTemplate.convertAndSend(ARTICLE_STATS_EXCHANGE, ARTICLE_STATS_KEY,
                    ArticleStatsMessage.builder()
                            .articleId(id)
                            .tagIds(tagIds)
                            .categoryId(article.getCategoryId())
                            .userId(article.getAuthorId())
                            .delta(-1)
                            .build());
            commonArticleService.sendSearchSyncMessage(OPERATION_DELETE, id);
        }

        commonArticleService.removeArticleCache(id, "管理端删除文章后清除缓存: articleId={}");
    }
    public void approveArticle(Long id) {
        commonArticleService.getArticleIfExists(id);

        articleService.lambdaUpdate()
                .eq(Article::getId, id)
                .set(Article::getReview, APPROVED)
                .set(Article::getUpdatedAt, LocalDateTime.now())
                .update();

        cacheUtil.evict(ARTICLE_DETAIL + id);
    }

    public void rejectArticle(Long id, String reason) {
        commonArticleService.getArticleIfExists(id);

        articleService.lambdaUpdate()
                .eq(Article::getId, id)
                .set(Article::getReview, REJECT)
                .set(Article::getUpdatedAt, LocalDateTime.now())
                .update();

        cacheUtil.evict(ARTICLE_DETAIL + id);
        // TODO: 可以发送通知给作者，告知拒绝原因

    }
    public void setArticleTop(Long id, Boolean isTop) {
        commonArticleService.getArticleIfExists(id);

        articleService.lambdaUpdate()
                .eq(Article::getId, id)
                .set(Article::getIsTop, isTop)
                .set(Article::getUpdatedAt, LocalDateTime.now())
                .update();

        // 清除缓存
        hotArticleCache.invalidateAll();
        articlePageCache.invalidateAll();
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
}
