package com.personblog.comment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.api.articleAPI.ArticleInfoAPI;
import com.personblog.api.interactionAPI.CommentApi;
import com.personblog.api.interactionAPI.LikeApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.comment.dto.AdminCommentQueryDTO;
import com.personblog.comment.dto.CommentCreateDTO;
import com.personblog.comment.entity.Comment;
import com.personblog.comment.mapper.CommentMapper;
import com.personblog.comment.service.ICommentService;
import com.personblog.comment.vo.*;
import com.personblog.common.dto.Comment.CommentNotificationMessage;
import com.personblog.common.dto.Interaction.LikeMessageDTO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.UserContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.personblog.common.config.mqConfig.CommentMqConfig.COMMENT_EXCHANGE;
import static com.personblog.common.config.mqConfig.CommentMqConfig.COMMENT_NOTIFICATION_KEY;
import static com.personblog.common.constant.RedisKeys.COMMENT_PAGE;
import static com.personblog.common.constant.StatusConstant.APPROVED;
import static com.personblog.common.constant.StatusConstant.PENDING;
import static com.personblog.common.constant.TargetTypeConstant.COMMENT;
import static com.personblog.common.enums.BizCodeEnum.*;

/**
 * <p>
 * 评论表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService, CommentApi {

    private final UseApi useApi;
    private final LikeApi likeApi;
    private final ArticleInfoAPI articleInfoAPI;
    private final RabbitTemplate rabbitTemplate;

    // 本地缓存 - 评论分页
    private Cache<String, Page<CommentVO>> commentPageCache;

    @PostConstruct
    public void initCommentCache() {
        commentPageCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats()
                .build();
    }
    @Override
    public Page<CommentVO> getCommentPage(Long articleId, Integer current, Integer size) {
        int page = (current == null || current <= 0) ? 1 : current;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 50);

        String cacheKey = COMMENT_PAGE + articleId + ":" + page + ":" + pageSize;

        // 查本地缓存
        Page<CommentVO> cached = commentPageCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        Page<CommentVO> result = queryCommentPage(articleId, page, pageSize);

        // 写入本地缓存
        commentPageCache.put(cacheKey, result);

        return result;
    }

    private Page<CommentVO> queryCommentPage(Long articleId, int page, int pageSize) {

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getArticleId, articleId)
                .eq(Comment::getStatus, APPROVED)
                .orderByDesc(Comment::getCreatedAt);

        Page<Comment> commentPage = this.page(new Page<>(page, pageSize), wrapper);

        List<CommentVO> voList = convertToVOList(commentPage.getRecords());

        Page<CommentVO> resultPage = new Page<>(page, pageSize, commentPage.getTotal());
        resultPage.setRecords(voList);

        return resultPage;
    }

    @Override
    public Set<Long> getIsLikes(List<Long> targetIds,String targetType) {
        return likeApi.batchIsLike(targetIds,targetType);
    }

    private List<CommentVO> convertToVOList(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> AllComments = comments.stream().map(Comment::getId).toList();
        Map<Long, Long> likesTime = likeApi.getLikesTime(AllComments, COMMENT);
        comments=comments.stream().filter(c-> c.getParentId()==null).toList();
        // 收集父评论作者 + 回复作者的所有用户ID，确保每个评论都能显示正确的头像昵称
        Set<Long> authorIds = new HashSet<>(
                comments.stream()
                        .map(Comment::getAuthorId)
                        .filter(Objects::nonNull)
                        .toList()
        );
        Map<Long, UserDTO> userMap;
        if (!authorIds.isEmpty()) {
            List<UserDTO> users = useApi.getUserInfo(authorIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        } else {
            userMap = new HashMap<>();
        }
        List<Long> commentIds = comments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
        Map<Long, List<Comment>> repliesMap = getRepliesMap(commentIds);

        // 补充收集回复者的用户ID（避免纯回复者无法显示头像昵称）
        for (List<Comment> replyList : repliesMap.values()) {
            for (Comment reply : replyList) {
                if (reply.getAuthorId() != null) {
                    authorIds.add(reply.getAuthorId());
                }
            }
        }
        // 如果有新增的回复者ID，补充查询用户信息
        if (authorIds.size() > userMap.size()) {
            List<UserDTO> extraUsers = useApi.getUserInfo(authorIds);
            for (UserDTO u : extraUsers) {
                userMap.putIfAbsent(u.getId(), u);
            }
        }
        return comments.stream().map(comment -> {
                CommentVO vo = new CommentVO();
                vo.setId(comment.getId());
                vo.setContent(comment.getContent());
                Long l = likesTime.get(comment.getId());
                vo.setLikes(l==null?comment.getLikes():l);
                vo.setCreatedAt(comment.getCreatedAt());

                UserDTO user = userMap.get(comment.getAuthorId());
                if (user != null) {
                    CommentVO.AuthorInfo authorInfo = new CommentVO.AuthorInfo();
                    authorInfo.setId(user.getId());
                    authorInfo.setName(user.getNickname());
                    authorInfo.setAvatar(user.getAvatar());
                    vo.setAuthor(authorInfo);
                }
                List<Comment> replies = repliesMap.get(comment.getId());
                if (replies != null && !replies.isEmpty()) {
                    vo.setReplies(convertRepliesToVO(replies, userMap,likesTime));
                }

                return vo;
            }).collect(Collectors.toList());
    }

    private Map<Long, List<Comment>> getRepliesMap(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Comment> replies = this.lambdaQuery()
                .in(Comment::getParentId, parentIds)
                .eq(Comment::getStatus, APPROVED)
                .orderByAsc(Comment::getCreatedAt)
                .list();

        return replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));
    }

    private List<CommentVO> convertRepliesToVO(List<Comment> replies, Map<Long, UserDTO> userMap,Map<Long,Long> likeTimes) {
        return replies.stream().map(reply -> {
            CommentVO vo = new CommentVO();
            vo.setId(reply.getId());
            vo.setContent(reply.getContent());
            Long l = likeTimes.get(reply.getId());
            vo.setLikes(l==null?reply.getLikes():l);
            vo.setCreatedAt(reply.getCreatedAt());

            UserDTO user = userMap.get(reply.getAuthorId());
            if (user != null) {
                CommentVO.AuthorInfo authorInfo = new CommentVO.AuthorInfo();
                authorInfo.setId(user.getId());
                authorInfo.setName(user.getNickname());
                authorInfo.setAvatar(user.getAvatar());
                vo.setAuthor(authorInfo);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLikeCount(List<LikeMessageDTO> dtoList) {
        List<Comment> list = new ArrayList<>(dtoList.size());
        for (LikeMessageDTO likeMessageDTO : dtoList) {
            Comment comment = new Comment();
            comment.setLikes(likeMessageDTO.getLikeTimes());
            comment.setId(likeMessageDTO.getId());
            list.add(comment);
        }
        updateBatchById(list);
    }

    @Override
    public Long getCommentAuthorId(Long commentId) {
        Comment comment = getById(commentId);
        return comment != null ? comment.getAuthorId() : null;
    }

    @Override
    public void updateReviewStatue(Long commentId, String statue) {
        boolean update = lambdaUpdate()
                .eq(Comment::getId, commentId)
                .set(Comment::getStatus, statue)
                .update();
        //TODO
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO createComment(CommentCreateDTO dto) {
        Long userId = dto.getUserId()!=null?dto.getUserId():UserContextHolder.getUserId();
        // 校验父评论是否存在（如果是回复）
        if (dto.getParentId() != null) {
            Comment parentComment = this.getById(dto.getParentId());
            if (parentComment == null || parentComment.getIsDeleted()) {
                throw new BizException(NOT_FOUND_PARENT_COMMENT);
            }
            // 确保回复的是同一篇文章的评论
            if (!parentComment.getArticleId().equals(dto.getArticleId())) {
                throw new BizException(BizCodeEnum.PARAMETER_ERROR);
            }
        }
        // 创建评论记录
        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setContent(dto.getContent().trim());
        comment.setAuthorId(userId);
        comment.setParentId(dto.getParentId());
        comment.setLikes(0L);
        comment.setStatus(PENDING);
        comment.setIsDeleted(false);
        comment.setIsAnonymous(false);

        boolean saved = this.save(comment);
        if (!saved) {
            throw new BizException(COMMENT_ERROR);
        }

        // 创建评论后清理分页缓存
        commentPageCache.invalidateAll();

        // 发送MQ消息异步处理：文章评论数更新 + 评论通知
        CommentNotificationMessage notificationMessage = CommentNotificationMessage.builder()
                .articleId(dto.getArticleId())
                .parentId(dto.getParentId())
                .content(dto.getContent().trim())
                .bizId(comment.getId())
                .commentId(comment.getId())
                .userId(userId)
                .articleTitle(dto.getArticleTitle())
                .delta(1)
                .build();
        rabbitTemplate.convertAndSend(COMMENT_EXCHANGE, COMMENT_NOTIFICATION_KEY, notificationMessage);

        CommentVO vo = toCommonVO(dto, comment);
        // 设置作者信息
        List<UserDTO> users = useApi.getUserInfo(Collections.singleton(userId));
        if (!users.isEmpty()) {
            UserDTO user = users.getFirst();
            CommentVO.AuthorInfo authorInfo = new CommentVO.AuthorInfo();
            authorInfo.setId(user.getId());
            authorInfo.setName(user.getNickname());
            authorInfo.setAvatar(user.getAvatar());
            vo.setAuthor(authorInfo);
        }

        return vo;
    }

    private CommentVO toCommonVO(CommentCreateDTO dto, Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setContent(comment.getContent());
        vo.setLikes(0L);
        vo.setCreatedAt(comment.getCreatedAt());
        if(dto.getTotalComment()==null){
            Long count = lambdaQuery()
                    .eq(Comment::getArticleId, dto.getArticleId())
                    .count();
            vo.setCommentTotal(count+1);
        }
        else vo.setCommentTotal(dto.getTotalComment()+1);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentRemoveVO deleteComment(Long id) {
        Long userId = UserContextHolder.getUserId();

        // 查询评论信息
        Comment comment = this.getById(id);
        if (comment == null || comment.getIsDeleted()) {
            throw new BizException(NOT_FOUND_PARENT_COMMENT);
        }

        // 权限校验：只能删除自己的评论或自己文章下的评论
        if (!comment.getAuthorId().equals(userId)) {
            throw new BizException(NO_POWER);
        }
        // 删除评论（逻辑删除，子评论也会被级联删除）
        List<Long> list = new ArrayList<>(lambdaQuery()
                .eq(Comment::getParentId, id)
                .eq(Comment::getIsDeleted, false)
                .eq(Comment::getStatus, APPROVED)
                .select(Comment::getId)
                .list().stream().map(Comment::getId).toList());
        list.add(id);
        boolean removed = this.removeByIds(list);
        int totalRemove=list.size();
        if (!removed) {
            throw new BizException(DELETE_ERROR);
        }

        // 发送MQ消息异步更新文章评论数
        CommentNotificationMessage notificationMessage = CommentNotificationMessage.builder()
                .articleId(comment.getArticleId())
                .delta(-totalRemove)
                .build();
        rabbitTemplate.convertAndSend(COMMENT_EXCHANGE, COMMENT_NOTIFICATION_KEY, notificationMessage);
        // 删除评论后清理分页缓存
        commentPageCache.invalidateAll();
        return CommentRemoveVO.builder().deleteTotal(totalRemove).build();
    }

    // ==================== 管理端接口实现 ====================

    @Override
    public Page<AdminCommentVO> getAdminCommentPage(AdminCommentQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        // 过滤 AI 智能助手的评论（authorId = -1）
        wrapper.ne(Comment::getAuthorId, -1L);

        // 关键词搜索：评论内容
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(Comment::getContent, dto.getKeyword());
        }

        // 审核状态筛选
        if (StrUtil.isNotBlank(dto.getStatus()) && !"all".equals(dto.getStatus())) {
            wrapper.eq(Comment::getStatus, dto.getStatus());
        }

        // 排序
        if ("likes".equals(dto.getSortField())) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Comment::getLikes);
        } else {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Comment::getCreatedAt);
        }

        Page<Comment> page = new Page<>(current, size);
        this.page(page, wrapper);

        // 批量获取用户信息
        List<Comment> comments = page.getRecords();
        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserDTO> userMap = getUserMap(authorIds);

        // 批量获取文章标题
        Set<Long> articleIds = comments.stream()
                .map(Comment::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> articleTitleMap = getArticleTitleMap(articleIds);

        // 转换为 VO
        Page<AdminCommentVO> voPage = new Page<>(current, size, page.getTotal());
        voPage.setRecords(comments.stream()
                .map(c -> toAdminCommentVO(c, userMap, articleTitleMap))
                .toList());
        return voPage;
    }

    @Override
    public AdminCommentDetailVO getAdminCommentDetail(Long id) {
        Comment comment = this.getById(id);
        if (comment == null || (comment.getAuthorId() != null && comment.getAuthorId() == -1L)) {
            throw new BizException(BizCodeEnum.COMMENT_NOT_EXIST);
        }

        // 获取评论者信息
        Set<Long> authorIds = new HashSet<>();
        authorIds.add(comment.getAuthorId());
        Map<Long, UserDTO> userMap = getUserMap(authorIds);

        // 获取文章标题
        Map<Long, String> articleTitleMap = getArticleTitleMap(
                Collections.singleton(comment.getArticleId()));

        // 构建详情 VO
        UserDTO author = userMap.get(comment.getAuthorId());
        AdminCommentDetailVO detail = AdminCommentDetailVO.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .authorName(author != null ? author.getNickname() : null)
                .authorAvatar(author != null ? author.getAvatar() : null)
                .authorEmail(author != null ? author.getEmail() : null)
                .content(comment.getContent())
                .targetType("article")
                .targetId(comment.getArticleId())
                .targetTitle(articleTitleMap.get(comment.getArticleId()))
                .status(comment.getStatus())
                .likes(comment.getLikes())
                .createdAt(comment.getCreatedAt())
                .reviewedAt(comment.getReviewedAt())
                .build();

        // 获取回复列表
        List<Comment> replies = this.lambdaQuery()
                .eq(Comment::getParentId, id)
                .orderByAsc(Comment::getCreatedAt)
                .list();

        if (!replies.isEmpty()) {
            Set<Long> replyAuthorIds = replies.stream()
                    .map(Comment::getAuthorId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, UserDTO> replyUserMap = getUserMap(replyAuthorIds);

            List<AdminCommentReplyVO> replyVOs = replies.stream()
                    .map(reply -> {
                        UserDTO replyUser = replyUserMap.get(reply.getAuthorId());
                        return AdminCommentReplyVO.builder()
                                .id(reply.getId())
                                .authorName(replyUser != null ? replyUser.getNickname() : null)
                                .authorAvatar(replyUser != null ? replyUser.getAvatar() : null)
                                .content(reply.getContent())
                                .createdAt(reply.getCreatedAt())
                                .build();
                    })
                    .toList();
            detail.setReplies(replyVOs);
        }

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveCommentByAdmin(Long id) {
        Comment comment = this.getById(id);
        if (comment == null) {
            throw new BizException(BizCodeEnum.COMMENT_NOT_EXIST);
        }
        lambdaUpdate()
                .eq(Comment::getId, id)
                .set(Comment::getStatus, APPROVED)
                .set(Comment::getReviewedAt, LocalDateTime.now())
                .update();
        commentPageCache.invalidateAll();
        log.info("管理员审核通过评论: commentId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectCommentByAdmin(Long id) {
        Comment comment = this.getById(id);
        if (comment == null) {
            throw new BizException(BizCodeEnum.COMMENT_NOT_EXIST);
        }
        lambdaUpdate()
                .eq(Comment::getId, id)
                .set(Comment::getStatus, "rejected")
                .set(Comment::getReviewedAt, LocalDateTime.now())
                .update();
        commentPageCache.invalidateAll();
        log.info("管理员审核拒绝评论: commentId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentByAdmin(Long id) {
        Comment comment = this.getById(id);
        if (comment == null) {
            throw new BizException(BizCodeEnum.COMMENT_NOT_EXIST);
        }
        // 同时删除子评论
        List<Long> childIds = this.lambdaQuery()
                .eq(Comment::getParentId, id)
                .select(Comment::getId)
                .list()
                .stream().map(Comment::getId).toList();
        List<Long> allIds = new ArrayList<>(childIds);
        allIds.add(id);
        this.removeByIds(allIds);
        commentPageCache.invalidateAll();
        log.info("管理员删除评论: commentId={}, 包含子评论数={}", id, childIds.size());
    }

    /**
     * 批量获取用户信息 Map
     */
    private Map<Long, UserDTO> getUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserDTO> users = useApi.getUserInfo(userIds);
        return users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
    }

    /**
     * 批量获取文章标题 Map
     */
    private Map<Long, String> getArticleTitleMap(Set<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>();
        for (Long articleId : articleIds) {
            try {
                String title = articleInfoAPI.getArticleTitle(articleId);
                map.put(articleId, title);
            } catch (Exception e) {
                log.warn("获取文章标题失败: articleId={}", articleId);
            }
        }
        return map;
    }

    /**
     * Comment 实体转 AdminCommentVO
     */
    private AdminCommentVO toAdminCommentVO(Comment comment, Map<Long, UserDTO> userMap, Map<Long, String> articleTitleMap) {
        UserDTO user = userMap.get(comment.getAuthorId());
        return AdminCommentVO.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .authorName(user != null ? user.getNickname() : null)
                .authorAvatar(user != null ? user.getAvatar() : null)
                .content(comment.getContent())
                .targetType("article")
                .targetId(comment.getArticleId())
                .targetTitle(articleTitleMap.get(comment.getArticleId()))
                .status(comment.getStatus())
                .likes(comment.getLikes())
                .createdAt(comment.getCreatedAt())
                .reviewedAt(comment.getReviewedAt())
                .build();
    }
}
