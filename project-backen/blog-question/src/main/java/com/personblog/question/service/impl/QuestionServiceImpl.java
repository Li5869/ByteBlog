package com.personblog.question.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.api.interactionAPI.LikeApi;
import com.personblog.api.interactionAPI.NotificationApi;
import com.personblog.api.questionAPI.QuestionApi;
import com.personblog.api.searchAPI.DeleteSearchAPI;
import com.personblog.api.searchAPI.SearchSyncApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.Interaction.LikeMessageDTO;
import com.personblog.common.dto.Notification.sse.NotificationMessageDTO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.entity.Tag;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.service.ITagService;
import com.personblog.common.sse.SseEmitterManager;
import com.personblog.common.utils.MultiLevelCacheUtil;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.question.dto.*;
import com.personblog.question.entity.Answer;
import com.personblog.question.entity.Question;
import com.personblog.question.entity.QuestionTag;
import com.personblog.question.mapper.QuestionMapper;
import com.personblog.question.service.IAnswerService;
import com.personblog.question.service.IQuestionService;
import com.personblog.question.service.IQuestionTagService;
import com.personblog.question.vo.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.personblog.common.constant.RedisKeys.*;
import static com.personblog.common.constant.TargetTypeConstant.ANSWER;
import static com.personblog.common.constant.TargetTypeConstant.QUESTION;

/**
 * 问题表 服务实现类
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements IQuestionService, QuestionApi {

    private final IQuestionTagService questionTagService;
    private final ITagService tagService;
    private final UseApi useApi;
    private final LikeApi likeApi;
    private final IAnswerService answerService;
    private final SearchSyncApi searchSyncApi;
    private final DeleteSearchAPI deleteSearchAPI;
    private final NotificationApi notificationApi;
    private final SseEmitterManager sseEmitterManager;
    private final MultiLevelCacheUtil cacheUtil;
    @Resource(name = "QuestionExecutor")
    private Executor questionExecutor;

    // 本地缓存 - 热门问题
    private Cache<String, List<HotQuestionVO>> hotQuestionCache;
    // 本地缓存 - 问题分页列表
    private Cache<String, Page<QuestionListVO>> questionPageCache;

    @PostConstruct
    public void initQuestionCaches() {
        hotQuestionCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();

        questionPageCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats()
                .build();
    }
    @Override
    public Page<QuestionListVO> getQuestionPage(QuestionQueryDTO queryDTO) {
        int current = (queryDTO.getCurrent() == null || queryDTO.getCurrent() <= 0) ? 1 : queryDTO.getCurrent();
        int size = (queryDTO.getSize() == null || queryDTO.getSize() <= 0) ? 10 : Math.min(queryDTO.getSize(), 50);
        if (queryDTO.getStatus() == null || queryDTO.getStatus().isEmpty()) {
            queryDTO.setStatus("all");
        }
        if (queryDTO.getSortBy() == null || queryDTO.getSortBy().isEmpty()) {
            queryDTO.setSortBy("newest");
        }

        // 构建缓存key：包含所有查询参数
        String cacheKey = QUESTION_PAGE + current + ":" + size
                + ":status:" + queryDTO.getStatus()
                + ":tag:" + queryDTO.getTagId()
                + ":sort:" + queryDTO.getSortBy()
                + ":kw:" + (queryDTO.getKeyword() != null ? queryDTO.getKeyword() : "");

        // 查本地缓存
        Page<QuestionListVO> cached = questionPageCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        Page<QuestionListVO> result = queryQuestionPage(queryDTO, current, size);

        // 写入本地缓存
        questionPageCache.put(cacheKey, result);

        return result;
    }

    private Page<QuestionListVO> queryQuestionPage(QuestionQueryDTO queryDTO, int current, int size) {

        List<Long> questionIdsByTag = null;
        if (queryDTO.getTagId() != null) {
            questionIdsByTag = questionTagService.lambdaQuery()
                    .eq(QuestionTag::getTagId, queryDTO.getTagId())
                    .list()
                    .stream()
                    .map(QuestionTag::getQuestionId)
                    .collect(Collectors.toList());
            if (questionIdsByTag.isEmpty()) {
                Page<QuestionListVO> emptyPage = new Page<>(current, size, 0L);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
        }

        List<Long> finalQuestionIdsByTag = questionIdsByTag;
        boolean isHotSort = "hot".equals(queryDTO.getSortBy()) || "hot".equals(queryDTO.getStatus());

        LambdaQueryWrapper<Question> baseWrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getIsDeleted, false)
                .in(finalQuestionIdsByTag != null, Question::getId, finalQuestionIdsByTag)
                .and("unanswered".equals(queryDTO.getStatus()), wrapper -> wrapper
                        .eq(Question::getIsSolved, false)
                        .eq(Question::getAnswers, 0))
                .and("solved".equals(queryDTO.getStatus()), wrapper -> wrapper
                        .eq(Question::getIsSolved, true))
                .and("hot".equals(queryDTO.getStatus()), wrapper -> wrapper
                        .and(w -> w.gt(Question::getViews, 0)
                                .or().gt(Question::getLikes, 0)
                                .or().gt(Question::getAnswers, 0)))
                .and(StrUtil.isNotBlank(queryDTO.getKeyword()), wrapper -> wrapper
                        .like(Question::getTitle, queryDTO.getKeyword())
                        .or()
                        .like(Question::getContent, queryDTO.getKeyword()));

        long total = count(baseWrapper);

        LambdaQueryWrapper<Question> queryWrapper = baseWrapper.clone();
        if (isHotSort) {
            queryWrapper.last("ORDER BY (views * 0.3 + likes * 0.5 + answers * 0.2) DESC, created_at DESC");
        } else {
            queryWrapper.orderByDesc(Question::getCreatedAt);
        }

        List<Question> questions = page(new Page<>(current, size), queryWrapper).getRecords();

        List<QuestionListVO> list = buildQuestionListVO(questions);

        if (!list.isEmpty()) {
            fillTagsForQuestionList(list);
        }

        Page<QuestionListVO> page = new Page<>(current, size, total);
        page.setRecords(list);
        return page;
    }

    private List<QuestionListVO> buildQuestionListVO(List<Question> questions) {
        if (CollectionUtil.isEmpty(questions)) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = questions.stream()
                .map(Question::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, UserDTO> userMap = new HashMap<>();
        try {
            List<UserDTO> users = useApi.getUserInfo(authorIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        } catch (Exception e) {
            log.warn("获取用户信息异常: {}", e.getMessage());
        }

        Map<Long, UserDTO> finalUserMap = userMap;
        return questions.stream().map(q -> {
            QuestionListVO vo = new QuestionListVO();
            vo.setId(q.getId());
            vo.setTitle(q.getTitle());
            vo.setExcerpt(q.getContent() != null && q.getContent().length() > 200
                    ? q.getContent().substring(0, 200)
                    : q.getContent());
            vo.setViews(q.getViews());
            vo.setAnswers(q.getAnswers());
            long likeCount = likeApi.getLikeCount(q.getId(), QUESTION);
            vo.setLikes(likeCount==0?q.getLikes():likeCount);
            vo.setIsSolved(q.getIsSolved());
            vo.setCreatedAt(q.getCreatedAt());

            UserDTO user = finalUserMap.get(q.getAuthorId());
            if (user != null) {
                AuthorInfoVO author = new AuthorInfoVO();
                author.setId(user.getId());
                author.setName(user.getNickname());
                author.setAvatar(user.getAvatar());
                vo.setAuthor(author);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public QuestionDetailVO getQuestionDetail(Long id) {
        String cacheKey = QUESTION_DETAIL + id;
        return cacheUtil.get(cacheKey, key -> loadQuestionDetailFromDB(id), 600, 120, QuestionDetailVO.class);
    }

    private QuestionDetailVO loadQuestionDetailFromDB(Long id) {
        Long userId = UserContextHolder.getUserId();
        if (id == null || id <= 0) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题ID无效");
        }

        Question question = lambdaQuery()
                .eq(Question::getId, id)
                .eq(Question::getIsDeleted, false)
                .one();

        if (question == null) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题不存在");
        }

        lambdaUpdate()
                .eq(Question::getId, id)
                .set(Question::getViews, question.getViews() + 1)
                .update();

        QuestionDetailVO detail = new QuestionDetailVO();
        detail.setId(question.getId());
        detail.setTitle(question.getTitle());
        detail.setContent(question.getContent());
        detail.setViews(question.getViews() + 1);
        detail.setAnswers(question.getAnswers());
        detail.setIsSolved(question.getIsSolved());
        detail.setCreatedAt(question.getCreatedAt() != null ? question.getCreatedAt().toString() : null);
        detail.setUpdatedAt(question.getUpdatedAt() != null ? question.getUpdatedAt().toString() : null);

        // 并行获取点赞数和点赞状态
        CompletableFuture<Long> likeCountFuture = CompletableFuture.supplyAsync(
                () -> likeApi.getLikeCount(id, QUESTION),
                questionExecutor
        );
        CompletableFuture<Boolean> isLikedFuture = CompletableFuture.supplyAsync(
                () -> userId != null && likeApi.isLiked(id, userId, QUESTION),
                questionExecutor
        );

        // 等待点赞相关结果
        try {
            Long likeCount = likeCountFuture.get();
            detail.setLikes(likeCount == 0 ? question.getLikes() : likeCount);
            detail.setIsLiked(isLikedFuture.get());
        } catch (Exception e) {
            log.warn("获取点赞信息异常: {}", e.getMessage());
            detail.setLikes(question.getLikes());
            detail.setIsLiked(false);
        }

        // 获取作者信息
        try {
            List<UserDTO> users = useApi.getUserInfo(Collections.singleton(question.getAuthorId()));
            if (!users.isEmpty()) {
                UserDTO user = users.getFirst();
                AuthorInfoVO author = new AuthorInfoVO();
                author.setId(user.getId());
                author.setName(user.getNickname());
                author.setAvatar(user.getAvatar());
                detail.setAuthor(author);
            }
        } catch (Exception e) {
            log.warn("获取用户信息异常: {}", e.getMessage());
        }

        // 填充标签信息（同步执行，因为需要返回给前端）
        fillTagsForQuestionDetail(detail);

        return detail;
    }

    private void evictQuestionCache() {
        hotQuestionCache.invalidateAll();
        questionPageCache.invalidateAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionCreateVO createQuestion(Long userId, QuestionCreateDTO dto) {
        // 1. 参数校验
        validateCreateDTO(dto);

        // 2. 校验标签是否存在
        validateTagIds(dto.getTagIds());

        LocalDateTime now = LocalDateTime.now();

        // 3. 保存问题记录
        Question question = new Question();
        question.setTitle(dto.getTitle().trim());
        question.setContent(StrUtil.trim(dto.getContent()));
        question.setAuthorId(userId);
        question.setViews(0L);
        question.setAnswers(0L);
        question.setLikes(0L);
        question.setIsSolved(false);
        question.setIsDeleted(false);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        boolean saved = save(question);
        if (!saved) {
            throw new BizException(BizCodeEnum.ADD_ERROR.getCode(), "发布问题失败");
        }

        // 4. 保存标签关联
        saveQuestionTags(question.getId(), dto.getTagIds());

        // 5. 异步处理：增加标签使用次数、同步ES索引
        Long questionId = question.getId();
        List<Long> tagIds = dto.getTagIds();
        CompletableFuture.runAsync(() -> increaseTagUseCount(tagIds,1), questionExecutor)
                .exceptionally(e -> {
                    log.error("增加标签使用次数失败, tagIds={}", tagIds, e);
                    return null;
                });
        CompletableFuture.runAsync(() -> tagService.invalidateTagCache(), questionExecutor)
                .exceptionally(e -> {
                    log.error("清理标签缓存失败", e);
                    return null;
                });
        CompletableFuture.runAsync(() -> searchSyncApi.syncQuestion(questionId), questionExecutor)
                .exceptionally(e -> {
                    log.error("同步问题到ES失败, questionId={}", questionId, e);
                    return null;
                });

        // 清理问题列表和热门缓存
        evictQuestionCache();

        // 6. 构建返回 VO
        return buildCreateVO(question, userId, dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long userId, Long questionId) {
        if (questionId == null || questionId <= 0) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题ID无效");
        }

        // 查询问题是否存在
        Question question = getById(questionId);
        if (question == null || Boolean.TRUE.equals(question.getIsDeleted())) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题不存在");
        }

        // 权限校验：只有提问者本人可以删除
        if (!Objects.equals(question.getAuthorId(), userId)) {
            throw new BizException(BizCodeEnum.NO_POWER);
        }

        // 逻辑删除问题
        boolean removed = removeById(questionId);
        if (!removed) {
            throw new BizException(BizCodeEnum.DELETE_ERROR.getCode(), "删除失败");
        }
        // 清理缓存
        cacheUtil.evict(QUESTION_DETAIL + questionId);
        evictQuestionCache();
        // 异步删除ES索引
        CompletableFuture.runAsync(() -> deleteSearchAPI.deleteQuestion(questionId), questionExecutor)
                .exceptionally(e -> {
                    log.error("删除问题ES索引失败, questionId={}", questionId, e);
                    return null;
                });
    }

    /**
     * 为问题列表批量填充标签信息
     */
    private void fillTagsForQuestionList(List<QuestionListVO> list) {
        List<Long> questionIds = list.stream()
                .map(QuestionListVO::getId)
                .collect(Collectors.toList());

        // 批量查询每个问题的标签ID
        List<QuestionTag> allQuestionTags = questionTagService.lambdaQuery()
                .in(QuestionTag::getQuestionId, questionIds)
                .list();

        if (CollectionUtil.isEmpty(allQuestionTags)) {
            return;
        }

        Set<Long> tagIds = allQuestionTags.stream()
                .map(QuestionTag::getTagId)
                .collect(Collectors.toSet());

        if (tagIds.isEmpty()) {
            return;
        }

        // 批量查询标签名称
        Map<Long, String> tagNameMap = tagService.listByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

        // 按 questionId 分组构建映射
        Map<Long, List<TagInfo>> questionTagMap = new HashMap<>();
        for (QuestionTag qt : allQuestionTags) {
            String tagName = tagNameMap.get(qt.getTagId());
            if (tagName != null) {
                TagInfo tagInfo = new TagInfo();
                tagInfo.setId(qt.getTagId());
                tagInfo.setName(tagName);
                questionTagMap.computeIfAbsent(qt.getQuestionId(), k -> new ArrayList<>()).add(tagInfo);
            }
        }

        // 填充到每个VO中
        for (QuestionListVO vo : list) {
            vo.setTags(questionTagMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }
    }

    /**
     * 为问题详情填充标签信息
     */
    private void fillTagsForQuestionDetail(QuestionDetailVO detail) {
        List<QuestionTag> tags = questionTagService.lambdaQuery()
                .eq(QuestionTag::getQuestionId, detail.getId())
                .list();

        if (CollectionUtil.isEmpty(tags)) {
            detail.setTags(Collections.emptyList());
            return;
        }

        List<Long> tagIdList = tags.stream().map(QuestionTag::getTagId).collect(Collectors.toList());

        List<Tag> tagList = tagService.listByIds(tagIdList);
        if (CollectionUtil.isNotEmpty(tagList)) {
            List<TagInfo> tagInfoList = tagList.stream().map(tag -> {
                TagInfo info = new TagInfo();
                info.setId(tag.getId());
                info.setName(tag.getName());
                return info;
            }).collect(Collectors.toList());
            detail.setTags(tagInfoList);
        } else {
            detail.setTags(Collections.emptyList());
        }
    }

    /**
     * 校验创建问题的参数
     */
    private void validateCreateDTO(QuestionCreateDTO dto) {
        if (dto == null) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "请求参数不能为空");
        }
        String title = StrUtil.trim(dto.getTitle());
        if (!StrUtil.isNotBlank(title)) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题标题不能为空");
        }
        if (title.length() > 100) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "标题长度不能超过100字符");
        }
        String content = StrUtil.trim(dto.getContent());
        if (!StrUtil.isNotBlank(content)) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题内容不能为空");
        }
        if (content.length() > 5000) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "内容长度不能超过5000字符");
        }
        if (dto.getTagIds() == null || dto.getTagIds().isEmpty()) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "请至少选择一个标签");
        }
        if (dto.getTagIds().size() > 5) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "最多选择5个标签");
        }
    }

    /**
     * 校验标签ID是否都存在
     */
    private void validateTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueIds = new HashSet<>(tagIds);
        long count = tagService.lambdaQuery().in(Tag::getId, uniqueIds).count();
        if (count != uniqueIds.size()) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "存在无效的标签ID");
        }
    }

    /**
     * 保存问题-标签关联关系
     */
    private void saveQuestionTags(Long questionId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<QuestionTag> relationList = tagIds.stream().map(tagId -> {
            QuestionTag relation = new QuestionTag();
            relation.setQuestionId(questionId);
            relation.setTagId(tagId);
            relation.setCreatedAt(LocalDateTime.now());
            return relation;
        }).collect(Collectors.toList());
        questionTagService.saveBatch(relationList);
    }

    /**
     * 增加标签使用次数
     */
    private void increaseTagUseCount(List<Long> tagIds,int delta) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Tag> tags = tagService.listByIds(new HashSet<>(tagIds));
        List<Tag> updateList = tags.stream().map(tag -> {
            Tag t = new Tag();
            t.setId(tag.getId());
            long current = tag.getUseCount() == null ? 0L : tag.getUseCount();
            t.setUseCount(current + delta);
            return t;
        }).collect(Collectors.toList());
        tagService.updateBatchById(updateList);
    }

    /**
     * 构建创建成功后的返回VO
     */
    private QuestionCreateVO buildCreateVO(Question question, Long userId, List<Long> tagIds) {
        QuestionCreateVO vo = new QuestionCreateVO();
        vo.setId(question.getId());
        vo.setTitle(question.getTitle());
        vo.setViews(0L);
        vo.setAnswers(0L);
        vo.setLikes(0L);
        vo.setIsSolved(false);
        vo.setCreatedAt(question.getCreatedAt());

        // 设置作者信息
        AuthorInfoVO authorVO = new AuthorInfoVO();
        try {
            List<UserDTO> users = useApi.getUserInfo(Collections.singleton(userId));
            if (!users.isEmpty()) {
                UserDTO user = users.getFirst();
                authorVO.setId(user.getId());
                authorVO.setName(user.getNickname());
                authorVO.setAvatar(user.getAvatar());
            }
        } catch (Exception e) {
            log.warn("获取用户信息异常: {}", e.getMessage());
        }
        vo.setAuthor(authorVO);

        // 设置标签信息
        if (tagIds != null && !tagIds.isEmpty()) {
            List<Tag> tags = tagService.listByIds(new HashSet<>(tagIds));
            List<TagInfo> tagInfoList = tags.stream().map(tag -> {
                TagInfo info = new TagInfo();
                info.setId(tag.getId());
                info.setName(tag.getName());
                return info;
            }).collect(Collectors.toList());
            vo.setTags(tagInfoList);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLikeCount(List<LikeMessageDTO> likeMessageDTOS) {
        List<Question> list = new ArrayList<>(likeMessageDTOS.size());
        for (LikeMessageDTO likeMessageDTO : likeMessageDTOS) {
            Question question = new Question();
            question.setLikes(likeMessageDTO.getLikeTimes());
            question.setId(likeMessageDTO.getId());
            list.add(question);
        }
        updateBatchById(list);
        // 清理问题详情缓存
        for (LikeMessageDTO dto : likeMessageDTOS) {
            cacheUtil.evict(QUESTION_DETAIL + dto.getId());
        }
    }

    @Override
    public Page<AnswerVO> getAnswerList(Long questionId, Integer current, Integer size, String sortBy) {
        if (questionId == null || questionId <= 0) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题ID无效");
        }

        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 20);
        int currentPage = (current == null || current <= 0) ? 1 : current;
        String sort = (sortBy == null || sortBy.isEmpty()) ? "best" : sortBy;

        Question question = getById(questionId);
        if (question == null || Boolean.TRUE.equals(question.getIsDeleted())) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题不存在");
        }

        LambdaQueryWrapper<Answer> queryWrapper = new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, questionId)
                .eq(Answer::getIsDeleted, false);

        switch (sort) {
            case "best" -> queryWrapper.orderByDesc(Answer::getIsBest)
                    .orderByDesc(Answer::getLikes)
                    .orderByDesc(Answer::getCreatedAt);
            case "newest" -> queryWrapper.orderByDesc(Answer::getCreatedAt);
            case "votes" -> queryWrapper.orderByDesc(Answer::getLikes)
                    .orderByDesc(Answer::getCreatedAt);
        }

        Page<Answer> answerPage = new Page<>(currentPage, pageSize);
        answerService.page(answerPage, queryWrapper);

        List<Answer> answers = answerPage.getRecords();
        List<AnswerVO> voList = buildAnswerVOList(answers);

        Page<AnswerVO> resultPage = new Page<>(currentPage, pageSize, answerPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private List<AnswerVO> buildAnswerVOList(List<Answer> answers) {
        if (CollectionUtil.isEmpty(answers)) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = answers.stream()
                .map(Answer::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, UserDTO> userMap = new HashMap<>();
        try {
            List<UserDTO> users = useApi.getUserInfo(authorIds);
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        } catch (Exception e) {
            log.warn("获取用户信息异常: {}", e.getMessage());
        }

        List<Long> answerIds = answers.stream()
                .map(Answer::getId)
                .collect(Collectors.toList());
        Set<Long> likedAnswerIds = Collections.emptySet();
        try {
            likedAnswerIds = likeApi.batchIsLike(answerIds, ANSWER);
        } catch (Exception e) {
            log.warn("获取点赞状态异常: {}", e.getMessage());
        }
        Map<Long, Long> likesTime = likeApi.getLikesTime(answerIds, ANSWER);
        Map<Long, UserDTO> finalUserMap = userMap;
        Set<Long> finalLikedAnswerIds = likedAnswerIds;
        return answers.stream().map(a -> {
            AnswerVO vo = new AnswerVO();
            vo.setId(a.getId());
            vo.setContent(a.getContent());
            vo.setLikes(likesTime.get(a.getId()));
            vo.setIsLiked(finalLikedAnswerIds.contains(a.getId()));
            vo.setIsBest(a.getIsBest());
            vo.setCreatedAt(a.getCreatedAt());

            UserDTO user = finalUserMap.get(a.getAuthorId());
            if (user != null) {
                AuthorInfoVO author = new AuthorInfoVO();
                author.setId(user.getId());
                author.setName(user.getNickname());
                author.setAvatar(user.getAvatar());
                vo.setAuthor(author);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnswerCreateVO createAnswer(Long userId, Long questionId, String content) {
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }

        if (questionId == null || questionId <= 0) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题ID无效");
        }

        String trimmedContent = StrUtil.trim(content);
        if (!StrUtil.isNotBlank(trimmedContent)) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "回答内容不能为空");
        }
        if (trimmedContent.length() > 5000) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "回答内容长度不能超过5000字符");
        }

        Question question = getById(questionId);
        if (question == null || Boolean.TRUE.equals(question.getIsDeleted())) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题不存在");
        }

        if (Objects.equals(question.getAuthorId(), userId)) {
            throw new BizException(BizCodeEnum.NO_POWER.getCode(), "不能对自己提出的问题进行回答");
        }

        boolean alreadyAnswered = answerService.lambdaQuery()
                .eq(Answer::getQuestionId, questionId)
                .eq(Answer::getAuthorId, userId)
                .eq(Answer::getIsDeleted, false)
                .exists();
        if (alreadyAnswered) {
            throw new BizException(BizCodeEnum.ADD_ERROR.getCode(), "您已经回答过这个问题");
        }

        LocalDateTime now = LocalDateTime.now();
        Answer answer = new Answer();
        answer.setContent(trimmedContent);
        answer.setQuestionId(questionId);
        answer.setAuthorId(userId);
        answer.setLikes(0L);
        answer.setIsBest(false);
        answer.setCreatedAt(now);
        answer.setUpdatedAt(now);
        boolean saved = answerService.save(answer);
        if (!saved) {
            throw new BizException(BizCodeEnum.ADD_ERROR.getCode(), "提交回答失败");
        }

        lambdaUpdate()
                .eq(Question::getId, questionId)
                .set(Question::getAnswers, question.getAnswers() + 1)
                .update();

        // 回答数变化，清理问题详情缓存
        cacheUtil.evict(QUESTION_DETAIL + questionId);
        evictQuestionCache();
        
        // 异步发送通知给问题作者
        Long questionAuthorId = question.getAuthorId();
        String questionTitle = question.getTitle();
        CompletableFuture.runAsync(() -> {
            try {
                // 获取回答者信息
                List<UserDTO> users = useApi.getUserInfo(Collections.singleton(userId));
                UserDTO sender = users.isEmpty() ? null : users.getFirst();
                
                // 构建通知消息
                NotificationMessageDTO messageDTO = NotificationMessageDTO.builder()
                        .userId(questionAuthorId)
                        .actionType("answer")
                        .targetType("question")
                        .targetId(questionId)
                        .senderId(userId)
                        .senderNickname(sender != null ? sender.getNickname() : "用户")
                        .senderAvatar(sender != null ? sender.getAvatar() : "")
                        .targetTitle(questionTitle)
                        .content(trimmedContent.length() > 100 ? trimmedContent.substring(0, 100) + "..." : trimmedContent)
                        .createdAt(LocalDateTime.now())
                        .build();
                // 保存通知到数据库，获取通知ID
                Long notificationId = notificationApi.saveNotification(messageDTO);
                
                // 设置通知ID后再发送SSE实时通知
                if (notificationId != null) {
                    messageDTO.setId(notificationId);
                    sseEmitterManager.sendToUser(questionAuthorId, messageDTO);
                }
            } catch (Exception e) {
                log.error("发送回答通知失败, questionId={}, userId={}", questionId, userId, e);
            }
        }, questionExecutor);
        
        return buildCreateAnswerVO(answer, userId);
    }

    private AnswerCreateVO buildCreateAnswerVO(Answer answer, Long userId) {
        AnswerCreateVO vo = new AnswerCreateVO();
        vo.setId(answer.getId());
        vo.setContent(answer.getContent());
        vo.setLikes(answer.getLikes());
        vo.setVoteCount(answer.getLikes());
        vo.setIsBest(answer.getIsBest());
        vo.setCreatedAt(answer.getCreatedAt());

        try {
            List<UserDTO> users = useApi.getUserInfo(Collections.singleton(userId));
            if (!users.isEmpty()) {
                UserDTO user = users.getFirst();
                AuthorInfoVO author = new AuthorInfoVO();
                author.setId(user.getId());
                author.setName(user.getNickname());
                author.setAvatar(user.getAvatar());
                vo.setAuthor(author);
            }
        } catch (Exception e) {
            log.warn("获取用户信息异常: {}", e.getMessage());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnswer(Long userId, Long answerId) {
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }

        if (answerId == null || answerId <= 0) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "回答ID无效");
        }

        Answer answer = answerService.lambdaQuery()
                .eq(Answer::getId, answerId)
                .eq(Answer::getIsDeleted, false)
                .one();

        if (answer == null) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "回答不存在");
        }

        if (!Objects.equals(answer.getAuthorId(), userId)) {
            throw new BizException(BizCodeEnum.NO_POWER);
        }

        boolean removed = answerService.removeById(answerId);
        if (!removed) {
            throw new BizException(BizCodeEnum.DELETE_ERROR.getCode(), "删除失败");
        }

        if (Boolean.TRUE.equals(answer.getIsBest())) {
                lambdaUpdate()
                        .eq(Question::getId, answer.getQuestionId())
                        .set(Question::getIsSolved, false)
                        .update();
        }

        Question question = getById(answer.getQuestionId());
        if (question != null && question.getAnswers() > 0) {
            lambdaUpdate()
                    .eq(Question::getId, answer.getQuestionId())
                    .set(Question::getAnswers, question.getAnswers() - 1)
                    .update();
        }

        // 回答数变化，清理问题详情缓存
        cacheUtil.evict(QUESTION_DETAIL + answer.getQuestionId());
        evictQuestionCache();
    }

    @Override
    public List<HotQuestionVO> getHotQuestions(Integer limit) {
        int maxLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 20);
        String cacheKey = QUESTION_HOT + ":" + maxLimit;

        // 查本地缓存
        List<HotQuestionVO> cached = hotQuestionCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Question> questions = lambdaQuery()
                .eq(Question::getIsDeleted, false)
                .ge(Question::getCreatedAt, thirtyDaysAgo)
                .orderByDesc(Question::getViews)
                .last("LIMIT " + maxLimit)
                .list();

        List<HotQuestionVO> result = questions.stream().map(q -> {
            HotQuestionVO vo = new HotQuestionVO();
            vo.setId(q.getId());
            String title = q.getTitle();
            vo.setTitle(title != null && title.length() > 50 ? title.substring(0, 50) : title);
            vo.setAnswers(q.getAnswers());
            vo.setViews(q.getViews());
            return vo;
        }).collect(Collectors.toList());

        // 写入本地缓存
        hotQuestionCache.put(cacheKey, result);

        return result;
    }

    @Override
    public Page<MyQuestionVO> getMyQuestions(Long userId, MyQuestionQueryDTO queryDTO) {
        int current = (queryDTO.getCurrent() == null || queryDTO.getCurrent() <= 0) ? 1 : queryDTO.getCurrent();
        int size = (queryDTO.getSize() == null || queryDTO.getSize() <= 0) ? 10 : Math.min(queryDTO.getSize(), 50);
        String status = (queryDTO.getStatus() == null || queryDTO.getStatus().isEmpty()) ? "all" : queryDTO.getStatus();
        String orderBy = (queryDTO.getOrderBy() == null || queryDTO.getOrderBy().isEmpty()) ? "created_at" : queryDTO.getOrderBy();

        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getAuthorId, userId)
                .eq(Question::getIsDeleted, false);

        // 状态筛选
        if ("solved".equals(status)) {
            queryWrapper.eq(Question::getIsSolved, true);
        } else if ("unsolved".equals(status)) {
            queryWrapper.eq(Question::getIsSolved, false);
        }

        // 排序
        switch (orderBy) {
            case "answers" -> queryWrapper.orderByDesc(Question::getAnswers);
            case "likes" -> queryWrapper.orderByDesc(Question::getLikes);
            default -> queryWrapper.orderByDesc(Question::getCreatedAt);
        }

        Page<Question> questionPage = new Page<>(current, size);
        page(questionPage, queryWrapper);

        List<Question> questions = questionPage.getRecords();
        List<MyQuestionVO> voList = buildMyQuestionVOList(questions);

        Page<MyQuestionVO> resultPage = new Page<>(current, size, questionPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private List<MyQuestionVO> buildMyQuestionVOList(List<Question> questions) {
        if (CollectionUtil.isEmpty(questions)) {
            return Collections.emptyList();
        }

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        // 批量查询标签
        Map<Long, List<String>> questionTagMap = new HashMap<>();
        try {
            List<QuestionTag> allQuestionTags = questionTagService.lambdaQuery()
                    .in(QuestionTag::getQuestionId, questionIds)
                    .list();

            if (CollectionUtil.isNotEmpty(allQuestionTags)) {
                Set<Long> tagIds = allQuestionTags.stream()
                        .map(QuestionTag::getTagId)
                        .collect(Collectors.toSet());

                Map<Long, String> tagNameMap = tagService.listByIds(tagIds).stream()
                        .collect(Collectors.toMap(Tag::getId, Tag::getName));

                for (QuestionTag qt : allQuestionTags) {
                    String tagName = tagNameMap.get(qt.getTagId());
                    if (tagName != null) {
                        questionTagMap.computeIfAbsent(qt.getQuestionId(), k -> new ArrayList<>()).add(tagName);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取问题标签异常: {}", e.getMessage());
        }

        return questions.stream().map(q -> {
            MyQuestionVO vo = new MyQuestionVO();
            vo.setId(q.getId());
            vo.setTitle(q.getTitle());
            vo.setContent(q.getContent());
            vo.setAnswers(q.getAnswers());
            vo.setViews(q.getViews());
            long likeCount = likeApi.getLikeCount(q.getId(), QUESTION);
            vo.setLikes(likeCount == 0 ? q.getLikes() : likeCount);
            vo.setIsSolved(q.getIsSolved());
            vo.setCreatedAt(q.getCreatedAt());
            vo.setTags(questionTagMap.getOrDefault(q.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<MyAnswerVO> getMyAnswers(Long userId, MyAnswerQueryDTO queryDTO) {
        int current = (queryDTO.getCurrent() == null || queryDTO.getCurrent() <= 0) ? 1 : queryDTO.getCurrent();
        int size = (queryDTO.getSize() == null || queryDTO.getSize() <= 0) ? 10 : Math.min(queryDTO.getSize(), 50);
        String type = (queryDTO.getType() == null || queryDTO.getType().isEmpty()) ? "all" : queryDTO.getType();
        String orderBy = (queryDTO.getOrderBy() == null || queryDTO.getOrderBy().isEmpty()) ? "created_at" : queryDTO.getOrderBy();

        LambdaQueryWrapper<Answer> queryWrapper = new LambdaQueryWrapper<Answer>()
                .eq(Answer::getAuthorId, userId)
                .eq(Answer::getIsDeleted, false);

        // 类型筛选
        if ("best".equals(type)) {
            queryWrapper.eq(Answer::getIsBest, true);
        } else if ("normal".equals(type)) {
            queryWrapper.eq(Answer::getIsBest, false);
        }

        // 排序
        if ("likes".equals(orderBy)) {
            queryWrapper.orderByDesc(Answer::getLikes);
        } else {
            queryWrapper.orderByDesc(Answer::getCreatedAt);
        }

        Page<Answer> answerPage = new Page<>(current, size);
        answerService.page(answerPage, queryWrapper);

        List<Answer> answers = answerPage.getRecords();
        List<MyAnswerVO> voList = buildMyAnswerVOList(answers);

        Page<MyAnswerVO> resultPage = new Page<>(current, size, answerPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private List<MyAnswerVO> buildMyAnswerVOList(List<Answer> answers) {
        if (CollectionUtil.isEmpty(answers)) {
            return Collections.emptyList();
        }

        // 获取问题ID列表
        Set<Long> questionIds = answers.stream()
                .map(Answer::getQuestionId)
                .collect(Collectors.toSet());

        // 批量查询问题标题
        Map<Long, String> questionTitleMap = new HashMap<>();
        try {
            List<Question> questions = lambdaQuery()
                    .in(Question::getId, questionIds)
                    .eq(Question::getIsDeleted, false)
                    .list();
            questionTitleMap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, Question::getTitle));
        } catch (Exception e) {
            log.warn("获取问题标题异常: {}", e.getMessage());
        }

        // 批量获取点赞数
        List<Long> answerIds = answers.stream()
                .map(Answer::getId)
                .collect(Collectors.toList());
        Map<Long, Long> likesTime = likeApi.getLikesTime(answerIds, ANSWER);

        Map<Long, String> finalQuestionTitleMap = questionTitleMap;
        return answers.stream().map(a -> {
            MyAnswerVO vo = new MyAnswerVO();
            vo.setId(a.getId());
            vo.setQuestionId(a.getQuestionId());
            vo.setQuestionTitle(finalQuestionTitleMap.get(a.getQuestionId()));
            vo.setContent(a.getContent());
            vo.setLikes(likesTime.get(a.getId()));
            vo.setIsBest(a.getIsBest());
            vo.setCreatedAt(a.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcceptAnswerVO acceptBestAnswer(Long userId, Long answerId) {
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }

        if (answerId == null || answerId <= 0) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "回答ID无效");
        }

        // 查询回答是否存在
        Answer answer = answerService.lambdaQuery()
                .eq(Answer::getId, answerId)
                .eq(Answer::getIsDeleted, false)
                .one();

        if (answer == null) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "回答不存在");
        }

        // 查询问题是否存在
        Question question = getById(answer.getQuestionId());
        if (question == null || Boolean.TRUE.equals(question.getIsDeleted())) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR.getCode(), "问题不存在");
        }

        // 权限校验：只有问题作者才能采纳最佳答案
        if (!Objects.equals(question.getAuthorId(), userId)) {
            throw new BizException(BizCodeEnum.NO_POWER);
        }

        // 如果该回答已经是最佳答案，直接返回
        if (Boolean.TRUE.equals(answer.getIsBest())) {
            AcceptAnswerVO vo = new AcceptAnswerVO();
            vo.setQuestionId(question.getId());
            vo.setAnswerId(answerId);
            vo.setIsSolved(question.getIsSolved());
            return vo;
        }

        // 如果问题已有其他最佳答案，先取消旧的最佳答案标记
        if (Boolean.TRUE.equals(question.getIsSolved())) {
            Answer oldBestAnswer = answerService.lambdaQuery()
                    .eq(Answer::getQuestionId, question.getId())
                    .eq(Answer::getIsBest, true)
                    .eq(Answer::getIsDeleted, false)
                    .one();

            if (oldBestAnswer != null) {
                oldBestAnswer.setIsBest(false);
                answerService.updateById(oldBestAnswer);
            }
        }

        // 设置新的最佳答案
        answer.setIsBest(true);
        boolean updated = answerService.updateById(answer);
        if (!updated) {
            throw new BizException(BizCodeEnum.UPDATE_ERROR.getCode(), "采纳失败");
        }

        // 更新问题的已解决状态
        lambdaUpdate()
                .eq(Question::getId, question.getId())
                .set(Question::getIsSolved, true)
                .update();

        // 采纳后清理问题详情缓存
        cacheUtil.evict(QUESTION_DETAIL + question.getId());

        // 异步发送采纳通知给回答者
        Long answerAuthorId = answer.getAuthorId();
        String questionTitle = question.getTitle();
        CompletableFuture.runAsync(() -> {
            try {
                // 不给自己发通知
                if (!answerAuthorId.equals(userId)) {
                    // 获取采纳者信息
                    List<UserDTO> users = useApi.getUserInfo(Collections.singleton(userId));
                    UserDTO sender = users.isEmpty() ? null : users.getFirst();
                    
                    // 构建通知消息
                    NotificationMessageDTO messageDTO = NotificationMessageDTO.builder()
                            .userId(answerAuthorId)
                            .actionType("adopt")
                            .targetType("answer")
                            .targetId(answerId)
                            .senderId(userId)
                            .senderNickname(sender != null ? sender.getNickname() : "用户")
                            .senderAvatar(sender != null ? sender.getAvatar() : "")
                            .targetTitle(questionTitle)
                            .createdAt(LocalDateTime.now())
                            .build();
                    
                    // 保存通知到数据库，获取通知ID
                    Long notificationId = notificationApi.saveNotification(messageDTO);
                    
                    // 设置通知ID后发送SSE实时通知
                    if (notificationId != null) {
                        messageDTO.setId(notificationId);
                        sseEmitterManager.sendToUser(answerAuthorId, messageDTO);
                    }
                }
            } catch (Exception e) {
                log.error("发送采纳通知失败, answerId={}, userId={}", answerId, userId, e);
            }
        }, questionExecutor);

        // 构建返回结果
        AcceptAnswerVO vo = new AcceptAnswerVO();
        vo.setQuestionId(question.getId());
        vo.setAnswerId(answerId);
        vo.setIsSolved(true);

        return vo;
    }
    // ==================== 管理端接口实现 ====================

    @Override
    public Page<AdminQuestionVO> getAdminQuestionPage(AdminQuestionQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索：问题标题
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(Question::getTitle, dto.getKeyword());
        }

        // 状态筛选
        if ("solved".equals(dto.getStatus())) {
            wrapper.eq(Question::getIsSolved, true);
        } else if ("pending".equals(dto.getStatus())) {
            wrapper.eq(Question::getIsSolved, false);
        }

        // 排序
        if ("view_count".equals(dto.getSortField())) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Question::getViews);
        } else if ("answer_count".equals(dto.getSortField())) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Question::getAnswers);
        } else {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Question::getCreatedAt);
        }

        Page<Question> page = new Page<>(current, size);
        this.page(page, wrapper);

        // 批量获取用户信息
        List<Question> questions = page.getRecords();
        Set<Long> authorIds = questions.stream()
                .map(Question::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserDTO> userMap = getUserMap(authorIds);

        // 转换为 VO
        Page<AdminQuestionVO> voPage = new Page<>(current, size, page.getTotal());
        voPage.setRecords(questions.stream()
                .map(q -> toAdminQuestionVO(q, userMap))
                .toList());
        return voPage;
    }

    @Override
    public AdminQuestionDetailVO getAdminQuestionDetail(Long id) {
        Question question = getById(id);
        if (question == null || Boolean.TRUE.equals(question.getIsDeleted())) {
            throw new BizException(BizCodeEnum.QUESTION_NOT_EXIST);
        }

        // 获取作者信息
        Set<Long> authorIds = new HashSet<>();
        authorIds.add(question.getAuthorId());
        Map<Long, UserDTO> userMap = getUserMap(authorIds);

        // 构建详情 VO
        UserDTO author = userMap.get(question.getAuthorId());
        AdminQuestionDetailVO detail = AdminQuestionDetailVO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())
                .authorId(question.getAuthorId())
                .authorName(author != null ? author.getNickname() : null)
                .authorAvatar(author != null ? author.getAvatar() : null)
                .answerCount(question.getAnswers())
                .viewCount(question.getViews())
                .status(Boolean.TRUE.equals(question.getIsSolved()) ? "solved" : "pending")
                .createdAt(question.getCreatedAt())
                .build();

        // 获取标签
        List<QuestionTag> questionTags = questionTagService.lambdaQuery()
                .eq(QuestionTag::getQuestionId, id)
                .list();
        if (!questionTags.isEmpty()) {
            Set<Long> tagIds = questionTags.stream()
                    .map(QuestionTag::getTagId)
                    .collect(Collectors.toSet());
            List<AdminQuestionTagVO> tagVOs = tagService.listByIds(tagIds).stream()
                    .map(tag -> AdminQuestionTagVO.builder()
                            .id(tag.getId())
                            .name(tag.getName())
                            .build())
                    .toList();
            detail.setTags(tagVOs);
        } else {
            detail.setTags(Collections.emptyList());
        }

        // 获取回答列表
        List<Answer> answers = answerService.lambdaQuery()
                .eq(Answer::getQuestionId, id)
                .eq(Answer::getIsDeleted, false)
                .orderByDesc(Answer::getIsBest)
                .orderByDesc(Answer::getLikes)
                .list();

        if (!answers.isEmpty()) {
            Set<Long> answerAuthorIds = answers.stream()
                    .map(Answer::getAuthorId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, UserDTO> answerUserMap = getUserMap(answerAuthorIds);

            List<AdminAnswerVO> answerVOs = answers.stream()
                    .map(a -> {
                        UserDTO answerUser = answerUserMap.get(a.getAuthorId());
                        return AdminAnswerVO.builder()
                                .id(a.getId())
                                .authorId(a.getAuthorId())
                                .authorName(answerUser != null ? answerUser.getNickname() : null)
                                .authorAvatar(answerUser != null ? answerUser.getAvatar() : null)
                                .content(a.getContent())
                                .likeCount(a.getLikes())
                                .isBest(a.getIsBest())
                                .createdAt(a.getCreatedAt())
                                .build();
                    })
                    .toList();
            detail.setAnswers(answerVOs);
        } else {
            detail.setAnswers(Collections.emptyList());
        }

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestionByAdmin(Long id) {
        Question question = getById(id);
        if (question == null || Boolean.TRUE.equals(question.getIsDeleted())) {
            throw new BizException(BizCodeEnum.QUESTION_NOT_EXIST);
        }
        // 逻辑删除问题
        this.removeById(id);

        // 同时逻辑删除该问题下的所有回答
        answerService.lambdaUpdate()
                .eq(Answer::getQuestionId, id)
                .set(Answer::getIsDeleted, true)
                .update();

        evictQuestionCache();
        log.info("管理员删除问题: questionId={}", id);
    }

    /**
     * 批量获取用户信息 Map
     */
    private Map<Long, UserDTO> getUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<UserDTO> users = useApi.getUserInfo(userIds);
            return users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        } catch (Exception e) {
            log.warn("获取用户信息异常: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Question 实体转 AdminQuestionVO
     */
    private AdminQuestionVO toAdminQuestionVO(Question question, Map<Long, UserDTO> userMap) {
        UserDTO user = userMap.get(question.getAuthorId());
        return AdminQuestionVO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .authorId(question.getAuthorId())
                .authorName(user != null ? user.getNickname() : null)
                .authorAvatar(user != null ? user.getAvatar() : null)
                .answerCount(question.getAnswers())
                .viewCount(question.getViews())
                .status(Boolean.TRUE.equals(question.getIsSolved()) ? "solved" : "pending")
                .createdAt(question.getCreatedAt())
                .build();
    }
}
