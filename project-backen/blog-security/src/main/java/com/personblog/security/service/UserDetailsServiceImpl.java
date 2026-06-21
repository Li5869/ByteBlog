package com.personblog.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.common.utils.RedisUtil;
import com.personblog.security.entity.LoginUser;
import com.personblog.security.entity.User;
import com.personblog.security.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.personblog.security.constant.RedisKeys.getUserLoginTokenKey;
import static com.personblog.security.constant.RedisKeys.getUserRefreshTokenKey;

/**
 * 用户详情服务实现类
 * <p>
 * 实现 Spring Security 的 UserDetailsService 接口
 * 负责加载用户信息、管理 Redis 中的登录状态
 * <p>
 * Redis 存储设计（精简后）：
 * - user:login:{userId} → token    （一个 key 同时承担 Token 校验 + 踢人机制）
 * - user:refresh_token:{userId} → refreshToken （Refresh Token 专用）
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final RedisUtil redisUtil;

    /**
     * 根据用户名加载用户详情
     * Spring Security 在认证时会调用此方法
     * 支持三种登录方式：用户名、邮箱、手机号
     *
     * @param username 用户名/邮箱/手机号
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 构建查询条件：支持用户名、邮箱、手机号三种方式登录
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                   .or()
                   .eq(User::getEmail, username)
                   .or()
                   .eq(User::getPhone, username);

        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 检查用户状态：0-封禁，1-正常
        if (user.getStatus() == 0) {
            throw new DisabledException("用户已被封禁");
        }

        // 构建 LoginUser 对象（仅保留核心字段）
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setIsAdmin(user.getIsAdmin());
        loginUser.setPermissions(new HashSet<>());

        // 如果是管理员，添加 admin 权限
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            loginUser.getPermissions().add("admin");
        }

        return loginUser;
    }

    // ==================== 登录状态管理（核心） ====================

    /**
     * 存储登录 Token 到 Redis
     * Key: user:login:{userId}，Value: token
     * 一个 key 同时承担 Token 校验和踢人机制
     *
     * @param token    Access Token
     * @param loginUser 登录用户信息（仅取 userId）
     */
    public void setLoginUser(String token, LoginUser loginUser) {
        String key = getUserLoginTokenKey(loginUser.getUserId());
        redisUtil.set(key, token, 30, TimeUnit.MINUTES);
    }

    /**
     * 根据 userId 和 token 验证登录状态并获取用户信息
     * 从 Redis 取出该 userId 存储的 token，与请求中的 token 比对
     * 一致则说明登录有效，再从数据库查询最新用户信息
     *
     * @param userId 用户 ID（从 JWT 中解析）
     * @param token  请求中的 Access Token
     * @return LoginUser 登录用户信息，无效则返回 null
     */
    public LoginUser getLoginUser(Long userId, String token) {
        String key = getUserLoginTokenKey(userId);
        Object obj = redisUtil.get(key);
        if (obj == null) {
            return null;
        }
        // 校验 token 是否与 Redis 中存储的一致（踢人机制：旧 token 会被新 token 覆盖）
        if (!token.equals(obj.toString())) {
            return null;
        }
        // 从数据库查询最新用户信息，确保权限、封禁状态实时生效
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            return null;
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setIsAdmin(user.getIsAdmin());
        loginUser.setPermissions(new HashSet<>());
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            loginUser.getPermissions().add("admin");
        }
        return loginUser;
    }

    /**
     * 刷新 Token 过期时间（活跃用户不过期）
     * 只要用户在 30 分钟内有请求，Token 就不会过期
     *
     * @param userId 用户 ID
     */
    public void refreshToken(Long userId) {
        String key = getUserLoginTokenKey(userId);
        redisUtil.expire(key, 30, TimeUnit.MINUTES);
    }

    /**
     * 删除登录状态（登出/踢人）
     *
     * @param userId 用户 ID
     */
    public void removeLoginUser(Long userId) {
        String key = getUserLoginTokenKey(userId);
        redisUtil.delete(key);
    }

    /**
     * 踢出用户（单点登录）
     * 新登录时调用，直接覆盖旧 token（新 set 的值会替换旧值）
     * 无需手动删除，setLoginUser 会自动覆盖
     *
     * @param userId 用户 ID
     */
    public void kickOutUser(Long userId) {
        log.info("踢出用户旧登录: userId={}", userId);
        removeLoginUser(userId);
    }

    // ==================== Refresh Token 管理 ====================

    /**
     * 存储 Refresh Token 到 Redis
     *
     * @param userId       用户 ID
     * @param refreshToken Refresh Token
     */
    public void setRefreshToken(Long userId, String refreshToken) {
        String key = getUserRefreshTokenKey(userId);
        redisUtil.set(key, refreshToken, 7, TimeUnit.DAYS);
    }

    /**
     * 从 Redis 获取 Refresh Token
     *
     * @param userId 用户 ID
     * @return Refresh Token 字符串，不存在则返回 null
     */
    public String getRefreshTokenByUserId(Long userId) {
        String key = getUserRefreshTokenKey(userId);
        Object obj = redisUtil.get(key);
        return obj != null ? obj.toString() : null;
    }

    /**
     * 删除 Refresh Token
     *
     * @param userId 用户 ID
     */
    public void removeRefreshToken(Long userId) {
        String key = getUserRefreshTokenKey(userId);
        redisUtil.delete(key);
    }
}
