package com.personblog.article.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.columnAPI.ColumnApi;
import com.personblog.api.searchAPI.ColumnSearchDataApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.article.dto.column.ColumnArticleDTO;
import com.personblog.article.dto.column.ColumnCreateDTO;
import com.personblog.article.dto.column.ColumnQueryDTO;
import com.personblog.article.dto.column.ColumnUpdateDTO;
import com.personblog.article.entity.Article;
import com.personblog.article.entity.Column;
import com.personblog.article.entity.ColumnArticle;
import com.personblog.article.mapper.ArticleMapper;
import com.personblog.article.mapper.ColumnMapper;
import com.personblog.article.service.IColumnArticleService;
import com.personblog.article.service.IColumnService;
import com.personblog.article.service.IColumnSubscriptionService;
import com.personblog.article.vo.*;
import com.personblog.common.dto.MqMessage.search.SearchSyncMessageDTO;
import com.personblog.common.dto.Search.ColumnSearchDTO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.personblog.common.constant.RedisKeys.COLUMN_READ_COUNT;
import static com.personblog.common.constant.TargetTypeConstant.COLUMN;
import static com.personblog.search.config.mqConfig.SearchMqConfig.*;

/**
 * 专栏服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnServiceImpl extends ServiceImpl<ColumnMapper, Column> implements IColumnService, ColumnApi, ColumnSearchDataApi {

    private final IColumnArticleService columnArticleService;
    private final IColumnSubscriptionService columnSubscriptionService;
    private final UseApi useApi;
    private final ArticleMapper articleMapper;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    /** 专栏文章数量上限 */
    private static final int MAX_ARTICLES_PER_COLUMN = 100;

    /**
     * 校验用户是否登录
     * @param userId 用户ID
     */
    private void validateLogin(Long userId) {
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
    }

    /**
     * 查询专栏并校验是否存在
     * @param columnId 专栏ID
     * @return 专栏实体
     */
    private Column getColumnOrThrow(Long columnId) {
        Column column = getById(columnId);
        if (column == null) {
            throw new BizException(BizCodeEnum.COLUMN_NOT_EXIST);
        }
        return column;
    }

    /**
     * 校验专栏权限（仅作者可操作）
     * @param column 专栏实体
     * @param userId 当前用户ID
     */
    private void validateColumnPermission(Column column, Long userId) {
        if (!column.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.COLUMN_NO_PERMISSION);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ColumnCreateVO createColumn(Long userId, ColumnCreateDTO dto) {
        // 校验用户登录
        validateLogin(userId);

        // 创建专栏实体
        Column column = new Column();
        column.setUserId(userId);
        column.setTitle(dto.getTitle());
        column.setDescription(dto.getDescription());
        column.setCover(dto.getCover());
        column.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        column.setArticlesCount(0);
        column.setSubscriptionCount(0);
        column.setViews(0L);
        column.setCreatedAt(LocalDateTime.now());
        column.setUpdatedAt(LocalDateTime.now());

        // 保存专栏
        save(column);

        // 已发布的专栏发送 MQ 消息同步到 ES 索引
        if (column.getStatus() == 1) {
            sendSearchSyncMessage(OPERATION_SYNC, column.getId());
        }

        // 返回结果
        return ColumnCreateVO.builder()
                .id(column.getId())
                .title(column.getTitle())
                .status(column.getStatus())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ColumnCreateVO updateColumn(Long userId, Long columnId, ColumnUpdateDTO dto) {
        // 校验用户登录
        validateLogin(userId);

        // 查询专栏并校验存在
        Column column = getColumnOrThrow(columnId);

        // 校验权限：仅作者可修改
        validateColumnPermission(column, userId);

        // 更新字段
        if (dto.getTitle() != null) {
            column.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            column.setDescription(dto.getDescription());
        }
        if (dto.getCover() != null) {
            column.setCover(dto.getCover());
        }
        if (dto.getStatus() != null) {
            column.setStatus(dto.getStatus());
        }
        column.setUpdatedAt(LocalDateTime.now());

        // 更新专栏
        updateById(column);

        // 已发布状态发送 MQ 消息同步到 ES 索引
        if (column.getStatus() == 1) {
            sendSearchSyncMessage(OPERATION_SYNC, columnId);
        }

        // 返回结果
        return ColumnCreateVO.builder()
                .id(column.getId())
                .title(column.getTitle())
                .status(column.getStatus())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteColumn(Long userId, Long columnId) {
        // 校验用户登录
        validateLogin(userId);

        // 查询专栏并校验存在
        Column column = getColumnOrThrow(columnId);

        // 校验权限：仅作者可删除
        validateColumnPermission(column, userId);

        // 删除专栏文章关联
        columnArticleService.remove(new LambdaQueryWrapper<ColumnArticle>()
                .eq(ColumnArticle::getColumnId, columnId));

        // 删除专栏订阅记录
        columnSubscriptionService.removeAllByColumnId(columnId);

        // 删除专栏（逻辑删除）
        removeById(columnId);

        // 发送 MQ 消息删除 ES 索引
        sendSearchSyncMessage(OPERATION_DELETE, columnId);
    }

    @Override
    public ColumnDetailVO getColumnDetail(Long columnId, Long currentUserId) {
        // 查询专栏并校验存在
        Column column = getColumnOrThrow(columnId);

        // 草稿状态仅作者可见
        if (column.getStatus() == 0 && !column.getUserId().equals(currentUserId)) {
            throw new BizException(BizCodeEnum.COLUMN_NOT_EXIST);
        }

        // 获取作者信息
        List<UserDTO> users = useApi.getUserInfo(Collections.singletonList(column.getUserId()));
        UserDTO author = CollectionUtil.isNotEmpty(users) ? users.getFirst() : null;

        // 获取专栏文章列表
        List<ColumnDetailVO.ColumnArticleVO> articles = getColumnArticles(columnId);

        // 检查当前用户是否已订阅
        boolean isSubscribed = false;
        if (currentUserId != null && !column.getUserId().equals(currentUserId)) {
            isSubscribed = columnSubscriptionService.exists(currentUserId, columnId);
        }
        // Redis 只存增量，实时浏览量 = 数据库基础值 + Redis增量
        redisTemplate.opsForHash().increment(COLUMN_READ_COUNT, columnId.toString(), 1);
        long view = column.getViews();
        Object browseCount = redisTemplate.opsForHash().get(COLUMN_READ_COUNT, columnId.toString());
        if (browseCount != null) {
            view += Long.parseLong(browseCount.toString());
        }

        // 构建返回对象
        return ColumnDetailVO.builder()
                .id(column.getId())
                .title(column.getTitle())
                .description(column.getDescription())
                .cover(column.getCover())
                .articlesCount(column.getArticlesCount())
                .subscriptionCount(column.getSubscriptionCount())
                .views(view)
                .status(column.getStatus())
                .createdAt(column.getCreatedAt())
                .updatedAt(column.getUpdatedAt())
                .authorId(column.getUserId())
                .authorName(author != null ? author.getNickname() : null)
                .authorAvatar(author != null ? author.getAvatar() : null)
                .isAuthor(column.getUserId().equals(currentUserId))
                .isSubscribed(isSubscribed)
                .articles(articles)
                .build();
    }

    @Override
    public Page<ColumnListVO> getColumnPage(ColumnQueryDTO dto) {
        // 设置分页参数
        int current = dto.getCurrent() != null ? dto.getCurrent() : 1;
        int size = dto.getSize() != null ? Math.min(dto.getSize(), 50) : 10;

        // 调用Mapper查询

        return baseMapper.selectColumnPage(new Page<>(current, size), dto);
    }

    @Override
    public List<MyColumnVO> getMyColumns(Long userId) {
        // 校验用户登录
        validateLogin(userId);

        // 查询我的专栏（包含草稿和已发布）
        List<Column> columns = list(new LambdaQueryWrapper<Column>()
                .eq(Column::getUserId, userId)
                .orderByDesc(Column::getUpdatedAt));

        // 转换为VO
        return columns.stream()
                .map(column -> MyColumnVO.builder()
                        .id(column.getId())
                        .title(column.getTitle())
                        .description(column.getDescription())
                        .cover(column.getCover())
                        .articlesCount(column.getArticlesCount())
                        .subscriptionCount(column.getSubscriptionCount())
                        .views(column.getViews())
                        .status(column.getStatus())
                        .createdAt(column.getCreatedAt())
                        .updatedAt(column.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public void updateColumnView() {
        Map<Object, Object> viewMap = redisTemplate.opsForHash().entries(COLUMN_READ_COUNT);
        
        if (viewMap.isEmpty()) {
            return;
        }
        
        List<Column> updateList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<Object, Object> entry : viewMap.entrySet()) {
            try {
                Long columnId = Long.parseLong(entry.getKey().toString());
                Long views = Long.parseLong(entry.getValue().toString());
                
                // Redis 中存的是增量，查询数据库当前值后叠加
                Column existColumn = getById(columnId);
                if (existColumn == null) {
                    continue;
                }
                
                Column column = new Column();
                column.setId(columnId);
                column.setViews(existColumn.getViews() + views);
                column.setUpdatedAt(now);
                updateList.add(column);
            } catch (Exception e) {
                log.error("解析专栏浏览量失败: key={}, value={}", entry.getKey(), entry.getValue(), e);
            }
        }
        
        if (!updateList.isEmpty()) {
            updateBatchById(updateList);
        }
        
        redisTemplate.delete(COLUMN_READ_COUNT);
    }

    /**
     * 获取专栏文章列表
     * @param columnId 专栏ID
     * @return 文章列表
     */
    private List<ColumnDetailVO.ColumnArticleVO> getColumnArticles(Long columnId) {
        // 调用Mapper查询专栏文章
        return baseMapper.selectColumnArticles(columnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addArticles(Long userId, Long columnId, ColumnArticleDTO dto) {
        // 校验用户登录
        validateLogin(userId);

        // 查询专栏并校验存在
        Column column = getColumnOrThrow(columnId);

        // 校验权限：仅作者可添加文章
        validateColumnPermission(column, userId);

        // 校验文章数量上限
        int currentCount = column.getArticlesCount() != null ? column.getArticlesCount() : 0;
        int addCount = dto.getArticleIds().size();
        if (currentCount + addCount > MAX_ARTICLES_PER_COLUMN) {
            throw new BizException(BizCodeEnum.COLUMN_ARTICLE_LIMIT_EXCEED);
        }

        // 校验文章是否已发布且属于当前用户
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .in(Article::getId, dto.getArticleIds())
                .eq(Article::getIsDeleted, false));

        // 过滤出符合条件的文章：已发布且属于当前用户
        Set<Long> validArticleIds = articles.stream()
                .filter(a -> a.getStatus() == 1 && a.getAuthorId().equals(userId))
                .map(Article::getId)
                .collect(Collectors.toSet());

        if (validArticleIds.isEmpty()) {
            throw new BizException(BizCodeEnum.COLUMN_PARAM_ERROR);
        }

        // 批量添加文章到专栏
        int addedCount = columnArticleService.batchAddArticles(columnId, new ArrayList<>(validArticleIds));

        // 更新专栏文章数量
        if (addedCount > 0) {
            column.setArticlesCount(currentCount + addedCount);
            column.setUpdatedAt(LocalDateTime.now());
            updateById(column);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeArticles(Long userId, Long columnId, ColumnArticleDTO dto) {
        // 校验用户登录
        validateLogin(userId);

        // 查询专栏并校验存在
        Column column = getColumnOrThrow(columnId);

        // 校验权限：仅作者可移除文章
        validateColumnPermission(column, userId);

        // 删除关联关系
        int removedCount = columnArticleService.batchRemoveArticles(columnId, dto.getArticleIds());

        // 更新专栏文章数量
        if (removedCount > 0) {
            int currentCount = column.getArticlesCount() != null ? column.getArticlesCount() : 0;
            column.setArticlesCount(Math.max(0, currentCount - removedCount));
            column.setUpdatedAt(LocalDateTime.now());
            updateById(column);
        }
    }

    @Override
    public List<MyArticleVO> getAvailableArticles(Long userId, Long columnId) {
        // 校验用户登录
        validateLogin(userId);

        // 调用Mapper查询可添加的文章
        return baseMapper.selectAvailableArticles(userId, columnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void subscribeColumn(Long userId, Long columnId) {
        // 校验用户登录
        validateLogin(userId);

        // 查询专栏并校验存在
        Column column = getColumnOrThrow(columnId);
        if (column.getStatus() != 1) {
            throw new BizException(BizCodeEnum.COLUMN_NOT_PUBLISHED);
        }

        // 不能订阅自己的专栏
        if (column.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.COLUMN_CANNOT_SUBSCRIBE_SELF);
        }

        // 检查是否已订阅
        if (columnSubscriptionService.exists(userId, columnId)) {
            throw new BizException(BizCodeEnum.COLUMN_ALREADY_SUBSCRIBED);
        }

        // 创建订阅记录（订阅数量由数据库触发器自动更新）
        columnSubscriptionService.subscribe(userId, columnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unsubscribeColumn(Long userId, Long columnId) {
        // 校验用户登录
        validateLogin(userId);

        // 删除订阅记录（订阅数量由数据库触发器自动更新）
        columnSubscriptionService.unsubscribe(userId, columnId);
    }

    @Override
    public Boolean checkSubscribed(Long userId, Long columnId) {
        // 未登录返回false
        if (userId == null) {
            return false;
        }

        // 检查是否已订阅
        return columnSubscriptionService.exists(userId, columnId);
    }

    @Override
    public List<SubscriptionVO> getSubscriptions(Long userId) {
        // 校验用户登录
        validateLogin(userId);

        // 调用Mapper查询订阅列表
        return baseMapper.selectSubscriptions(userId);
    }

    @Override
    public List<SubscriberVO> getSubscribers(Long columnId) {
        // 查询专栏并校验存在
        getColumnOrThrow(columnId);

        // 调用Mapper查询订阅用户列表
        return baseMapper.selectSubscribers(columnId);
    }

    @Override
    public List<ColumnListVO> getHotColumns(int limit) {
        return baseMapper.selectHotColumns(limit);
    }

    // ==================== ColumnSearchDataApi 实现 ====================

    @Override
    public List<ColumnSearchDTO> listAllColumnsForSearch() {
        // 查询所有已发布且未删除的专栏
        List<Column> columns = list(new LambdaQueryWrapper<Column>()
                .eq(Column::getStatus, 1)
                .eq(Column::getIsDeleted, false));

        if (columns.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询作者信息
        Set<Long> userIds = columns.stream()
                .map(Column::getUserId)
                .collect(Collectors.toSet());
        List<UserDTO> users = useApi.getUserInfo(userIds);
        Map<Long, UserDTO> userMap = users.stream()
                .collect(Collectors.toMap(UserDTO::getId, u -> u));

        // 转换为搜索DTO
        return columns.stream()
                .map(column -> {
                    UserDTO user = userMap.get(column.getUserId());
                    return ColumnSearchDTO.builder()
                            .id(column.getId())
                            .title(column.getTitle())
                            .description(column.getDescription())
                            .cover(column.getCover())
                            .userId(column.getUserId())
                            .authorName(user != null ? user.getNickname() : null)
                            .authorAvatar(user != null ? user.getAvatar() : null)
                            .articlesCount(column.getArticlesCount())
                            .subscriptionCount(column.getSubscriptionCount())
                            .views(column.getViews())
                            .status(column.getStatus())
                            .createdAt(column.getCreatedAt())
                            .updatedAt(column.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    public ColumnSearchDTO getColumnForSearch(Long columnId) {
        Column column = getById(columnId);
        // 专栏不存在或已删除，返回null由搜索模块移除索引
        if (column == null || Boolean.TRUE.equals(column.getIsDeleted())) {
            return null;
        }
        // 草稿状态也返回null，移除索引
        if (column.getStatus() != 1) {
            return null;
        }

        // 查询作者信息
        List<UserDTO> users = useApi.getUserInfo(Collections.singletonList(column.getUserId()));
        UserDTO user = CollectionUtil.isNotEmpty(users) ? users.getFirst() : null;

        return ColumnSearchDTO.builder()
                .id(column.getId())
                .title(column.getTitle())
                .description(column.getDescription())
                .cover(column.getCover())
                .userId(column.getUserId())
                .authorName(user != null ? user.getNickname() : null)
                .authorAvatar(user != null ? user.getAvatar() : null)
                .articlesCount(column.getArticlesCount())
                .subscriptionCount(column.getSubscriptionCount())
                .views(column.getViews())
                .status(column.getStatus())
                .createdAt(column.getCreatedAt())
                .updatedAt(column.getUpdatedAt())
                .build();
    }

    // ==================== MQ 消息发送 ====================

    /**
     * 发送搜索同步消息到 MQ
     *
     * @param operation 操作类型：sync-同步，delete-删除
     * @param columnId  专栏ID
     */
    private void sendSearchSyncMessage(String operation, Long columnId) {
        try {
            SearchSyncMessageDTO message = SearchSyncMessageDTO.builder()
                    .operation(operation)
                    .targetType(COLUMN)
                    .targetId(columnId)
                    .build();
            rabbitTemplate.convertAndSend(SEARCH_EXCHANGE, SEARCH_SYNC_KEY, message);
            log.info("发送专栏搜索同步消息成功: operation={}, columnId={}", operation, columnId);
        } catch (Exception e) {
            log.error("发送专栏搜索同步消息失败: operation={}, columnId={}", operation, columnId, e);
        }
    }
}
