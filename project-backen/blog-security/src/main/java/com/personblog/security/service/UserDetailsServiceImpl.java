package com.personblog.security.service;

import cn.hutool.core.bean.BeanUtil;
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

import static com.personblog.security.constant.RedisKeys.*;

/**
 * 用户详情服务实现类
 * 
 * 实现 Spring Security 的 UserDetailsService 接口
 * 负责加载用户信息、管理 Redis 中的登录状态
 * 
 * 核心功能：
 * 1. loadUserByUsername: 根据用户名/邮箱/手机号加载用户信息
 * 2. getLoginUserByToken: 从 Redis 获取登录用户信息
 * 3. setLoginUser: 将登录用户信息存入 Redis
 * 4. refreshToken: 刷新 Token 过期时间
 * 5. removeLoginUser: 删除登录状态（登出）
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
     * 流程：
     * 1. 根据用户名/邮箱/手机号查询用户
     * 2. 检查用户是否存在
     * 3. 检查用户是否被封禁
     * 4. 构建 LoginUser 对象并设置权限
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
        
        // 查询用户
        User user = userMapper.selectOne(queryWrapper);
        
        // 用户不存在
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
        // 权限用于 @PreAuthorize("hasAuthority('admin')") 等注解
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            loginUser.getPermissions().add("admin");
        }
        
        return loginUser;
    }
    
    /**
     * 根据 Token 从 Redis 获取登录用户信息
     * 在 JWT 过滤器中调用，用于获取已登录用户的信息
     * 
     * @param token JWT Token
     * @return LoginUser 登录用户信息，不存在则返回 null
     */
    public LoginUser getLoginUserByToken(String token) {
        // 构建 Redis Key：user:token:{token}
        String key = USER_TOKEN + token;
        // 从 Redis 获取
        Object obj = redisUtil.get(key);
        if(obj!=null){
           return BeanUtil.toBean(obj,LoginUser.class);
        }
        return null;
    }
    
    /**
     * 将登录用户信息存入 Redis
     * 在用户登录成功后调用，建立会话
     * 同时设置过期时间，实现自动过期
     * 注意：存储前会清除密码等敏感信息，避免冗余和安全风险
     * 
     * @param token JWT Token
     * @param loginUser 登录用户信息
     */
    public void setLoginUser(String token, LoginUser loginUser) {
        // 清除密码，避免敏感信息存储到 Redis
        loginUser.setPassword(null);
        // 构建 Redis Key
        String key = USER_TOKEN + token;
        // 存入 Redis，设置 30 分钟过期
        redisUtil.set(key, loginUser, 30, TimeUnit.MINUTES);
    }
    
    /**
     * 刷新 Token 过期时间
     * 在用户请求时调用，实现"活跃用户不过期"的效果
     * 只要用户在 30 分钟内有请求，Token 就不会过期
     * 同时刷新用户当前登录 Token 映射的过期时间，确保踢人机制同步
     * 
     * @param token JWT Token
     */
    public void refreshToken(String token) {
        // 1. 刷新 Access Token 的过期时间
        String key = USER_TOKEN + token;
        redisUtil.expire(key, 30, TimeUnit.MINUTES);
        
        // 2. 获取用户信息，刷新用户当前登录 Token 映射的过期时间
        LoginUser loginUser = getLoginUserByToken(token);
        if (loginUser != null) {
            String loginKey = getUserLoginTokenKey(loginUser.getUserId());
            redisUtil.expire(loginKey, 30, TimeUnit.MINUTES);
        }
    }
    
    /**
     * 删除登录状态（登出）
     * 在用户登出时调用，从 Redis 中删除登录信息
     * 这样即使 Token 未过期，也无法再使用
     * 
     * @param token JWT Token
     */
    public void removeLoginUser(String token) {
        // 构建 Redis Key
        String key = USER_TOKEN + token;
        // 从 Redis 删除
        redisUtil.delete(key);
    }

    /**
     * 存储 Refresh Token 到 Redis
     * 在登录或刷新 Token 成功后调用，建立 Refresh Token 会话
     * 与 JWT 中的 Refresh Token 形成双验证（Redis + JWT）
     * 
     * @param userId 用户ID
     * @param refreshToken Refresh Token
     */
    public void setRefreshToken(Long userId, String refreshToken) {
        // 构建 Redis Key：user:refresh_token:{userId}
        String key = getUserRefreshTokenKey(userId);
        // 存入 Redis，设置 7 天过期（与配置的 refresh-token-expiration 一致）
        redisUtil.set(key, refreshToken, 7, TimeUnit.DAYS);
    }

    /**
     * 从 Redis 获取 Refresh Token
     * 用于在刷新 Token 时验证前端提交的 Refresh Token
     * 是否与 Redis 中存储的一致
     * 
     * @param userId 用户ID
     * @return Refresh Token 字符串，不存在则返回 null
     */
    public String getRefreshTokenByUserId(Long userId) {
        // 构建 Redis Key：user:refresh_token:{userId}
        String key = getUserRefreshTokenKey(userId);
        Object obj = redisUtil.get(key);
        return obj != null ? obj.toString() : null;
    }

    /**
     * 删除 Refresh Token
     * 在 Token 刷新（一次性使用）或登出时调用
     * 实现 Refresh Token Rotation 机制
     * 
     * @param userId 用户ID
     */
    public void removeRefreshToken(Long userId) {
        // 构建 Redis Key：user:refresh_token:{userId}
        String key = getUserRefreshTokenKey(userId);
        // 从 Redis 删除
        redisUtil.delete(key);
    }

    /**
     * 获取用户当前登录的 Token
     * 用于实现踢人机制，检查用户是否已在其他地方登录
     * 
     * @param userId 用户ID
     * @return 当前登录的 Token，不存在则返回 null
     */
    public String getCurrentTokenByUserId(Long userId) {
        // 构建 Redis Key：user:login:{userId}
        String key = getUserLoginTokenKey(userId);
        // 从 Redis 获取
        Object obj = redisUtil.get(key);
        return obj != null ? obj.toString() : null;
    }

    /**
     * 设置用户当前登录的 Token
     * 在用户登录成功后调用，建立用户ID与Token的映射关系
     * 用于实现踢人机制
     * 
     * @param userId 用户ID
     * @param token 当前登录的 Access Token
     */
    public void setCurrentToken(Long userId, String token) {
        // 构建 Redis Key：user:login:{userId}
        String key = getUserLoginTokenKey(userId);
        // 存入 Redis，设置 30 分钟过期（与 Access Token 一致）
        redisUtil.set(key, token, 30, TimeUnit.MINUTES);
    }

    /**
     * 踢出用户（删除旧登录）
     * 在用户重新登录时调用，删除旧的登录状态
     * 实现一个账号只能在一个地方登录
     * 
     * @param userId 用户ID
     */
    public void kickOutUser(Long userId) {
        // 1. 获取用户当前的 Token
        String oldToken = getCurrentTokenByUserId(userId);
        
        if (oldToken != null) {
            // 2. 删除旧的 Token 登录状态
            removeLoginUser(oldToken);
            log.info("踢出用户旧登录: userId={}, oldToken={}", userId, oldToken);
        }
        
        // 3. 删除用户ID到Token的映射
        String loginKey = getUserLoginTokenKey(userId);
        redisUtil.delete(loginKey);
    }

    /**
     * 删除用户当前登录 Token 映射
     * 在用户登出时调用，清理用户ID到Token的映射关系
     * 
     * @param userId 用户ID
     */
    public void removeCurrentToken(Long userId) {
        // 构建 Redis Key：user:login:{userId}
        String key = getUserLoginTokenKey(userId);
        // 从 Redis 删除
        redisUtil.delete(key);
    }
}
