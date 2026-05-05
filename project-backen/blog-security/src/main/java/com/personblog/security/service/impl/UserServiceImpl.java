package com.personblog.security.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.api.interactionAPI.FollowApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.dto.User.UserLikeMessageDTO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.MultiLevelCacheUtil;
import com.personblog.security.dto.AdminUserQueryDTO;
import com.personblog.security.dto.UpdateProfileDTO;
import com.personblog.security.entity.User;
import com.personblog.security.mapper.UserMapper;
import com.personblog.security.service.IUserService;
import com.personblog.security.vo.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.personblog.common.constant.PageConstant.DEFAULT_ACTIVE_SIZE;
import static com.personblog.common.constant.PageConstant.MAX_ACTIVE_SIZE;
import static com.personblog.common.constant.RedisKeys.USER_AUTHOR;
import static com.personblog.common.constant.RedisKeys.USER_INFO;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService, UseApi {

    private final FollowApi followService;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MultiLevelCacheUtil cacheUtil;

    // 用户信息本地缓存
    private Cache<Long, UserDTO> userInfoCache;

    @PostConstruct
    public void initCache() {
        userInfoCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }


    @Override
    public UserProfileStatsVO getUserProfileStats(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST);
        }

        UserProfileStatsVO vo = new UserProfileStatsVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setEmail(user.getEmail());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        UserProfileStatsVO.Stats stats = getStats(user);
        vo.setStats(stats);

        return vo;
    }

    private  UserProfileStatsVO.Stats getStats(User user) {
        UserProfileStatsVO.Stats stats = new UserProfileStatsVO.Stats();
        stats.setArticleCount(Optional.ofNullable(user.getArticlesCount()).orElse(0L));
        stats.setFanCount(Optional.ofNullable(user.getFansCount()).orElse(0L));
        stats.setLikeReceivedCount(Optional.ofNullable(user.getLikesCount()).orElse(0L));
        stats.setFollowingCount(Optional.ofNullable(user.getFollowingCount()).orElse(0L));
        stats.setCollectionCount(Optional.ofNullable(user.getCollectionsCount()).orElse(0L));
        stats.setViewCount(Optional.ofNullable(user.getViewsCount()).orElse(0L));
        stats.setCommentCount(Optional.ofNullable(user.getCommentsCount()).orElse(0L));
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST);
        }

        User updateUser = new User();
        updateUser.setId(userId);

        if (dto.getNickname() != null) {
            updateUser.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            updateUser.setAvatar(dto.getAvatar());
        }
        if (dto.getBio() != null) {
            updateUser.setBio(dto.getBio());
        }
        if(dto.getPhone()!=null){
            updateUser.setPhone(dto.getPhone());
        }
        if(dto.getGender()!=null){
            updateUser.setGender(dto.getGender());
        }
        if(dto.getEmail()!=null){
            updateUser.setEmail(dto.getEmail());
        }
        updateUser.setUpdatedAt(LocalDateTime.now());
        // 清除缓存
        userInfoCache.invalidate(userId);
        cacheUtil.evict(USER_INFO + userId);
        cacheUtil.evict(USER_AUTHOR + userId);
        log.info("更新用户资料后清除缓存: userId={}", userId);
        this.updateById(updateUser);
    }

    /**
     * 获取活跃博主列表
     * 按文章数量降序排序，返回文章数量最多的用户
     * @param size 返回数量，默认4，最大20
     * @return 活跃博主列表
     */
    @Override
    public List<ActiveUserVO> getActiveUsers(Integer size) {
        int limit = (size == null || size <= 0) ? DEFAULT_ACTIVE_SIZE : Math.min(size, MAX_ACTIVE_SIZE);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1)
                .orderByDesc(User::getArticlesCount)
                .last("LIMIT " + limit);

        List<User> users = this.list(wrapper);
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<Long> followIds = followService.checkBatchFollowStatus(userIds);
        return users.stream().map(user -> {
            ActiveUserVO vo = new ActiveUserVO();
            vo.setId(user.getId());
            vo.setName(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setArticles(Optional.ofNullable(user.getArticlesCount()).orElse(0L));
            vo.setFollowers(Optional.ofNullable(user.getFansCount()).orElse(0L));
            vo.setIsFollowing(followIds.contains(user.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取作者信息
     * 缓存策略：Redis 缓存，10分钟
     */
    @Override
    public AuthorInfoVO getAuthorInfo(Long userId) {
        String cacheKey = USER_AUTHOR + userId;
        return cacheUtil.get(cacheKey, key->loadAuthorInfoFromDB(userId), 200, 500, AuthorInfoVO.class);
    }

    private AuthorInfoVO loadAuthorInfoFromDB(Long userId) {
        // 查询数据库
        User user = this.getById(userId);
        if (user == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST);
        }

        AuthorInfoVO vo = new AuthorInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender() != null ? user.getGender().intValue() : null);
        vo.setIsAdmin(user.getIsAdmin());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setArticleCount(Optional.ofNullable(user.getArticlesCount()).orElse(0L));
        vo.setFollowCount(Optional.ofNullable(user.getFollowingCount()).orElse(0L));
        vo.setFansCount(Optional.ofNullable(user.getFansCount()).orElse(0L));
        vo.setLikeCount(Optional.ofNullable(user.getLikesCount()).orElse(0L));
        vo.setCollectionCount(Optional.ofNullable(user.getCollectionsCount()).orElse(0L));
        return vo;
    }

    /**
     * 批量获取用户信息
     * 缓存策略：双级缓存（Caffeine + Redis），5分钟
     */
    @Override
    public List<UserDTO> getUserInfo(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserDTO> result = new ArrayList<>(userIds.size());
        // 查缓存
        for (Long userId : userIds) {
            String key =USER_INFO+userId;
            UserDTO userDTO = cacheUtil.get(key, s -> getUserInfo(userId), 600, 200, UserDTO.class);
            result.add(userDTO);
        }
        return result;
    }

    private UserDTO getUserInfo(Long userId){
        return BeanUtil.toBean(getById(userId),UserDTO.class);
    }
    @Override
    public void updateFanCount(Long userId, int delta) {
       lambdaUpdate()
               .eq(User::getId,userId)
               .setSql("fans_count=fans_count+"+delta)
               .update();
       // 粉丝数变化，清理缓存
       cacheUtil.evict(USER_INFO + userId);
       cacheUtil.evict(USER_AUTHOR + userId);
    }

    @Override
    public void updateFollowingCount(Long userId, int delta) {
        lambdaUpdate()
                .eq(User::getId,userId)
                .setSql("following_count=following_count+"+delta)
                .update();
        // 关注数变化，清理缓存
        cacheUtil.evict(USER_INFO + userId);
        cacheUtil.evict(USER_AUTHOR + userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateLikesCount(List<UserLikeMessageDTO> dtoList) {
        userMapper.batchUpdateLikeCount(dtoList);
        // 点赞数变化，清理相关用户缓存
        for (UserLikeMessageDTO dto : dtoList) {
            if (dto.getAuthorId() != null) {
                cacheUtil.evict(USER_INFO + dto.getAuthorId());
                cacheUtil.evict(USER_AUTHOR + dto.getAuthorId());
            }
        }
    }

    @Override
    public void updateCollectionsCount(Long userId, int delta) {
        lambdaUpdate()
                .eq(User::getId, userId)
                .setSql("collections_count=collections_count+" + delta)
                .update();
        // 收藏数变化，清理缓存
        cacheUtil.evict(USER_INFO + userId);
        cacheUtil.evict(USER_AUTHOR + userId);
    }

    @Override
    @Async("ArticleCountExecutor")
    public void updateArticlesCount(Long userId, int delta) {
        lambdaUpdate()
                .eq(User::getId, userId)
                .setSql("articles_count=articles_count+" + delta)
                .update();
        // 文章数变化，清理缓存
        cacheUtil.evict(USER_INFO + userId);
        cacheUtil.evict(USER_AUTHOR + userId);
    }

    // ==================== 管理端接口实现 ====================

    @Override
    public Page<AdminUserVO> getAdminUserPage(AdminUserQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索：用户名、昵称、邮箱
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.and(w -> w
                    .like(User::getUsername, dto.getKeyword())
                    .or().like(User::getNickname, dto.getKeyword())
                    .or().like(User::getEmail, dto.getKeyword())
            );
        }

        // 状态筛选
        if (StrUtil.isNotBlank(dto.getStatus()) && !"all".equals(dto.getStatus())) {
            Short statusVal = "banned".equals(dto.getStatus()) ? (short) 0 : (short) 1;
            wrapper.eq(User::getStatus, statusVal);
        }

        // 排序
        if ("articles_count".equals(dto.getSortField())) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), User::getArticlesCount);
        } else if ("fans_count".equals(dto.getSortField())) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), User::getFansCount);
        } else {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), User::getCreatedAt);
        }

        Page<User> page = new Page<>(current, size);
        this.page(page, wrapper);

        // 转换为 VO
        Page<AdminUserVO> voPage = new Page<>(current, size, page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(this::toAdminUserVO)
                .toList());
        return voPage;
    }

    @Override
    public AdminUserDetailVO getAdminUserDetail(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST);
        }

        AdminUserDetailVO vo = AdminUserDetailVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bio(user.getBio())
                .gender(convertGender(user.getGender()))
                .status(convertStatus(user.getStatus()))
                .articleCount(Optional.ofNullable(user.getArticlesCount()).orElse(0L))
                .fansCount(Optional.ofNullable(user.getFansCount()).orElse(0L))
                .followCount(Optional.ofNullable(user.getFollowingCount()).orElse(0L))
                .likeCount(Optional.ofNullable(user.getLikesCount()).orElse(0L))
                .lastLoginAt(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .lastLoginIp(user.getLastLoginIp())
                .isAdmin(user.getIsAdmin())
                .build();
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserByAdmin(Long id, User user) {
        User existing = this.getById(id);
        if (existing == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST);
        }

        User updateUser = new User();
        updateUser.setId(id);
        if (user.getNickname() != null) {
            updateUser.setNickname(user.getNickname());
        }
        if (user.getEmail() != null) {
            updateUser.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            updateUser.setPhone(user.getPhone());
        }
        if (user.getBio() != null) {
            updateUser.setBio(user.getBio());
        }
        if (user.getGender() != null) {
            updateUser.setGender(user.getGender());
        }
        updateUser.setUpdatedAt(LocalDateTime.now());
        this.updateById(updateUser);

        // 清除缓存
        userInfoCache.invalidate(id);
        cacheUtil.evict(USER_INFO + id);
        cacheUtil.evict(USER_AUTHOR + id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatusByAdmin(Long id, Short status) {
        User existing = this.getById(id);
        if (existing == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST);
        }

        lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getStatus, status)
                .set(User::getUpdatedAt, LocalDateTime.now())
                .update();

        // 清除缓存
        userInfoCache.invalidate(id);
        cacheUtil.evict(USER_INFO + id);
        cacheUtil.evict(USER_AUTHOR + id);
        log.info("管理员更新用户状态: userId={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserByAdmin(Long id) {
        User existing = this.getById(id);
        if (existing == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST);
        }
        this.removeById(id);

        // 清除缓存
        userInfoCache.invalidate(id);
        cacheUtil.evict(USER_INFO + id);
        cacheUtil.evict(USER_AUTHOR + id);
        log.info("管理员删除用户: userId={}", id);
    }

    /**
     * User 实体转 AdminUserVO
     * 处理 gender(Short->String) 和 status(Short->String) 的转换
     */
    private AdminUserVO toAdminUserVO(User user) {
        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bio(user.getBio())
                .gender(convertGender(user.getGender()))
                .status(convertStatus(user.getStatus()))
                .articleCount(Optional.ofNullable(user.getArticlesCount()).orElse(0L))
                .fansCount(Optional.ofNullable(user.getFansCount()).orElse(0L))
                .followCount(Optional.ofNullable(user.getFollowingCount()).orElse(0L))
                .likeCount(Optional.ofNullable(user.getLikesCount()).orElse(0L))
                .lastLoginAt(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 性别 Short 转 String：0=保密, 1=男, 2=女
     */
    private String convertGender(Short gender) {
        if (gender == null || gender == 0) return "保密";
        if (gender == 1) return "男";
        if (gender == 2) return "女";
        return "保密";
    }

    /**
     * 状态 Short 转 String：0=banned, 1=normal
     */
    private String convertStatus(Short status) {
        if (status == null || status == 0) return "banned";
        return "normal";
    }
}
