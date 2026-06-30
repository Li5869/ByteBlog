package com.personblog.article.BizService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.adminAPI.TagApi;
import com.personblog.api.adminAPI.vo.TagVO;
import com.personblog.api.interactionAPI.FollowApi;
import com.personblog.api.interactionAPI.LikeApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.article.dto.article.ArticleQueryDTO;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.entity.Category;
import com.personblog.article.mapper.ArticleMapper;
import com.personblog.article.service.IArticleService;
import com.personblog.article.service.IArticleTagService;
import com.personblog.article.service.ICategoryService;
import com.personblog.article.vo.Article.ArticleListVO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.personblog.article.config.cachConfig.ArticleCacheConfig.articlePageCache;
import static com.personblog.article.constant.RedisKeys.ARTICLE_PAGE;
import static com.personblog.common.constant.PageConstant.*;
import static com.personblog.common.constant.RedisKeys.BROWSE_COUNT_KEY;
import static com.personblog.common.constant.StatusConstant.APPROVED;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleListBizService {
    private final ICategoryService categoryService;
    private final IArticleTagService articleTagService;
    private final TagApi tagApi;
    private final IArticleService articleService;
    private final UseApi useApi;
    private final FollowApi followApi;
    private final LikeApi likeApi;
    private final ArticleMapper articleMapper;
    private final StringRedisTemplate redisTemplate;
    private final CommonArticleService commonArticleService;
    public Page<ArticleListVO> getArticlePage(ArticleQueryDTO queryDTO) {
        int[] pageParams = commonArticleService.normalizePageParams(queryDTO.getCurrent(), queryDTO.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int current = pageParams[0];
        int size = pageParams[1];

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

    public List<ArticleListVO> getRandomArticles(Integer size) {
        int limit = (size == null || size <= 0) ? DEFAULT_RANDOM_SIZE : Math.min(size, MAX_RANDOM_SIZE);

        List<Article> articles =articleMapper.selectRandomArticles(limit);

        return convertToVOList(articles);
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
        commonArticleService.applyOrderBy(wrapper, orderBy);
        Page<Article> articlePage = articleService.page(new Page<>(current, size), wrapper);

        Page<ArticleListVO> resultPage = new Page<>(current, size, articlePage.getTotal());

        List<ArticleListVO> voList = convertToVOList(articlePage.getRecords());
        resultPage.setRecords(voList);

        return resultPage;
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
}
