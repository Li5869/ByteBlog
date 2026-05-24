package com.personblog.article.BizService;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.AIAPI.AiArticleDraftApi;
import com.personblog.api.AIwritingAPI.WritingTaskApi;
import com.personblog.api.adminAPI.TagApi;
import com.personblog.article.dto.article.ArticlePublishDTO;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.ArticleTag;
import com.personblog.article.entity.Category;
import com.personblog.article.service.IArticleService;
import com.personblog.article.service.IArticleTagService;
import com.personblog.article.service.ICategoryService;
import com.personblog.article.service.IColumnArticleService;
import com.personblog.article.vo.Article.ArticleEditVO;
import com.personblog.article.vo.Article.ArticlePublishVO;
import com.personblog.article.vo.Article.MyArticleVO;
import com.personblog.common.dto.MqMessage.AIModerate.AiModerateMessage;
import com.personblog.common.dto.MqMessage.article.ArticleStatsMessage;
import com.personblog.common.dto.Tag.TagDTO;
import com.personblog.common.exception.BizException;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.personblog.ai.config.mqConfig.AiMqConfig.AI_EXCHANGE;
import static com.personblog.ai.config.mqConfig.AiMqConfig.AI_MODERATE_KEY;
import static com.personblog.article.config.mqConfig.ArticleStatsMqConfig.ARTICLE_STATS_EXCHANGE;
import static com.personblog.article.config.mqConfig.ArticleStatsMqConfig.ARTICLE_STATS_KEY;
import static com.personblog.common.constant.StatusConstant.PENDING;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.common.enums.BizCodeEnum.*;
import static com.personblog.search.config.mqConfig.SearchMqConfig.OPERATION_DELETE;
import static com.personblog.search.config.mqConfig.SearchMqConfig.OPERATION_SYNC;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticlePublishBizService {
    private final ICategoryService categoryService;
    private final IArticleTagService articleTagService;
    private final TagApi tagApi;
    private final IArticleService articleService;
    private final RabbitTemplate rabbitTemplate;
    private final WritingTaskApi writingTaskApi;
    private final AiArticleDraftApi draftApi;
    private final IColumnArticleService columnArticleService;
    private final CommonArticleService commonArticleService;
    @Resource(name = "ArticleCountExecutor")
    private Executor articleCountExecutor;
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
        boolean save = articleService.save(article);
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
            commonArticleService.sendSearchSyncMessage(OPERATION_SYNC, articleId);
            commonArticleService.removeArticleCache(articleId,"创建文章后删除缓存");
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


    @Transactional(rollbackFor = Exception.class)
    public ArticlePublishVO updateArticle(Long userId, Long articleId, ArticlePublishDTO dto) {
        validatePublishDTO(dto);
        validateCategory(dto.getCategoryId());
        Set<Long> tagIds = resolveTagIds(dto.getTagIds(), dto.getTagNames());

        Article oldArticle = commonArticleService.getAndValidateArticle(articleId, userId);

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
        boolean b = articleService.updateById(article);
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
            commonArticleService.sendSearchSyncMessage(OPERATION_SYNC, articleId);
            // 清除相关缓存
            commonArticleService.removeArticleCache(articleId, "更新文章后清除缓存: articleId={}");
        }
        ArticlePublishVO vo = new ArticlePublishVO();
        vo.setId(articleId);
        vo.setStatus(dto.getStatus());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long userId, Long articleId) {
        Article article = commonArticleService.getAndValidateArticle(articleId, userId);
        Set<Long> tagIds = commonArticleService.getTagIdsByArticleId(articleId);
        boolean removeById = articleService.removeById(articleId);
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
            commonArticleService.sendSearchSyncMessage(OPERATION_DELETE, articleId);
            CompletableFuture.runAsync(()->columnArticleService.removeArticleFromAllColumns(articleId),articleCountExecutor);
            commonArticleService.removeArticleCache(articleId, "删除文章后清除缓存: articleId={}");
        }
    }

    public Page<MyArticleVO> getMyArticles(Long userId, Integer current, Integer size, Integer status, String orderBy) {
        int[] pageParams = commonArticleService.normalizePageParams(current, size, 10, 50);
        int pageNum = pageParams[0];
        int pageSize = pageParams[1];

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getAuthorId, userId)
                .eq(Article::getIsDeleted, false);

        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }

        commonArticleService.applyOrderBy(wrapper, orderBy);

        Page<Article> articlePage = articleService.page(new Page<>(pageNum, pageSize), wrapper);

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

    public ArticleEditVO getEditArticle(Long userId, Long articleId) {
        Article article = commonArticleService.getAndValidateArticle(articleId, userId);

        ArticleEditVO vo = new ArticleEditVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setCover(article.getCover());
        vo.setCategoryId(article.getCategoryId());
        vo.setStatus(article.getStatus());

        vo.setTagIds(new ArrayList<>(commonArticleService.getTagIdsByArticleId(articleId)));
        return vo;
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
}
