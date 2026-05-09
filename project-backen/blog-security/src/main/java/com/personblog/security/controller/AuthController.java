package com.personblog.security.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.api.searchAPI.SearchSyncApi;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.monitor.BusinessMetrics;
import com.personblog.common.result.JsonData;
import com.personblog.security.dto.LoginDTO;
import com.personblog.security.dto.RefreshTokenDTO;
import com.personblog.security.dto.RegisterDTO;
import com.personblog.security.entity.LoginUser;
import com.personblog.security.entity.User;
import com.personblog.security.mapper.UserMapper;
import com.personblog.security.service.UserDetailsServiceImpl;
import com.personblog.security.utils.JwtUtil;
import com.personblog.security.vo.RefreshTokenVO;
import com.personblog.security.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 认证控制器
 * 
 * 处理用户认证相关请求：登录、注册、登出、获取用户信息
 * 
 * @author LSH
 */
@Slf4j
@Tag(name = "认证管理", description = "用户登录、注册、登出等接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SearchSyncApi searchSyncApi;
    private final BusinessMetrics businessMetrics;
    @Resource(name = "ArticleCountExecutor")
    private Executor articleCountExecutor;
    
    /**
     * 用户登录
     * <p>
     * 登录流程：
     * 1. 创建认证令牌（包含用户名和密码）
     * 2. 调用 AuthenticationManager 进行认证
     * 3. 认证成功后生成 JWT Token
     * 4. 将用户信息存入 Redis
     * 5. 返回用户信息和 Token
     * 
     * @param loginDTO 登录请求参数
     * @return 用户信息和 Token
     */
    @Operation(summary = "用户登录", description = "通过用户名/邮箱/手机号和密码登录")
    @PostMapping("/login")
    public JsonData<UserInfoVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("用户登录请求: {}", loginDTO.getUsername());
        
        try {
            // 1. 先检查用户是否被封禁（避免 Spring Security 异常包装导致错误信息不准确）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, loginDTO.getUsername())
                    .or()
                    .eq(User::getEmail, loginDTO.getUsername())
                    .or()
                    .eq(User::getPhone, loginDTO.getUsername());
            User checkUser = userMapper.selectOne(queryWrapper);
            if (checkUser != null && checkUser.getStatus() == 0) {
                log.warn("登录失败 - 账号已被封禁: {}", loginDTO.getUsername());
                return JsonData.buildResult(BizCodeEnum.ACCOUNT_DISABLED);
            }

            // 2. 创建认证令牌（包含用户名和密码）
            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());

            // 3. 调用 AuthenticationManager 进行认证
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            
            // 4. 认证成功，获取用户信息
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            // 5. 踢人机制：检查用户是否已在其他地方登录，如果已登录则踢掉旧登录
            userDetailsService.kickOutUser(loginUser.getUserId());

            // 6. 生成 Access Token 和 Refresh Token（双 Token）
            String token = jwtUtil.generateToken(loginUser.getUserId());
            String refreshToken = jwtUtil.generateRefreshToken(loginUser.getUserId());
            
            // 7. 将登录用户信息存入 Redis（Access Token 30分钟过期）
            userDetailsService.setLoginUser(token, loginUser);
            // 将 Refresh Token 存入 Redis（7天过期，支持服务端主动失效）
            userDetailsService.setRefreshToken(loginUser.getUserId(), refreshToken);
            // 设置用户当前登录的 Token（用于踢人机制）
            userDetailsService.setCurrentToken(loginUser.getUserId(), token);
            
            // 8. 从数据库查询完整用户信息用于返回
            User user = userMapper.selectById(loginUser.getUserId());
            
            // 9. 构建返回的用户信息（包含 Access Token 和 Refresh Token）
            UserInfoVO userInfoVO = new UserInfoVO();
            userInfoVO.setId(user.getId());
            userInfoVO.setUsername(user.getUsername());
            userInfoVO.setNickname(user.getNickname());
            userInfoVO.setAvatar(user.getAvatar());
            userInfoVO.setIsAdmin(user.getIsAdmin());
            userInfoVO.setToken(token);
            userInfoVO.setRefreshToken(refreshToken);
            
            // 10. 更新最后登录时间
            User updateUser = new User();
            updateUser.setId(loginUser.getUserId());
            updateUser.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(updateUser);
            
            log.info("用户登录成功: userId={}, username={}", loginUser.getUserId(), loginUser.getUsername());
            
            return JsonData.buildSuccess(userInfoVO);
            
        } catch (BadCredentialsException e) {
            // 用户名或密码错误
            log.warn("登录失败 - 用户名或密码错误: {}", loginDTO.getUsername());
            return JsonData.buildResult(BizCodeEnum.LOGIN_ERROR);
            
        } catch (DisabledException e) {
            // 账号被禁用
            log.warn("登录失败 - 账号被禁用: {}", loginDTO.getUsername());
            return JsonData.buildResult(BizCodeEnum.ACCOUNT_DISABLED);
            
        } catch (Exception e) {
            // 其他认证失败
            log.error("登录失败 - 系统错误: {}", e.getMessage(), e);
            return JsonData.buildResult(BizCodeEnum.LOGIN_ERROR);
        }
    }
    
    /**
     * 用户注册
     * 
     * 注册流程：
     * 1. 检查用户名是否已存在
     * 2. 创建用户对象
     * 3. 加密密码
     * 4. 保存到数据库
     * 
     * @param registerDTO 注册请求参数
     * @return 成功
     */
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public JsonData<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        log.info("用户注册请求: {}", registerDTO.getUsername());
        
        // 1. 检查用户名是否已存在
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername())
        );
        if (count > 0) {
            return JsonData.buildResult(BizCodeEnum.USER_REPEAT);
        }
        
        // 2. 创建用户对象
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setNickname(registerDTO.getNickname());
        user.setStatus((short) 1);
        user.setIsAdmin(false);
        user.setArticlesCount(0L);
        user.setFansCount(0L);
        user.setFollowingCount(0L);
        user.setViewsCount(0L);
        user.setLikesCount(0L);
        user.setCollectionsCount(0L);
        user.setCommentsCount(0L);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // 3. 保存到数据库
        userMapper.insert(user);
        
        // 4. 异步同步作者信息到ES
        Long authorId = user.getId();
        CompletableFuture.runAsync(() -> searchSyncApi.syncAuthor(authorId), articleCountExecutor)
                .exceptionally(e -> {
                    log.error("同步作者到ES失败, authorId={}", authorId, e);
                    return null;
                });
        
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        businessMetrics.recordUserRegister();

        return JsonData.buildSuccess();
    }

    /**
     * 刷新 Token
     * <p>
     * 使用 Refresh Token 获取新的 Access Token 和 Refresh Token
     * 采用一次性刷新机制（Rotation）：旧的 Refresh Token 立即失效
     * <p>
     * 刷新流程：
     * 1. 验证 Refresh Token 的 JWT 签名和有效期
     * 2. 从 Refresh Token 解析用户ID
     * 3. 校验 Redis 中存储的 Refresh Token 是否一致（防重放）
     * 4. 删除旧的 Refresh Token（一次性使用）
     * 5. 生成新的 Access Token 和 Refresh Token
     * 6. 将新 Token 存入 Redis
     * 7. 返回新 Token
     * 
     * @param dto 包含 Refresh Token 的请求体
     * @return 新的 Access Token 和 Refresh Token
     */
    @Operation(summary = "刷新Token", description = "使用Refresh Token获取新的Access Token（一次性刷新机制）")
    @PostMapping("/refresh")
    public JsonData<RefreshTokenVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        String refreshToken = dto.getRefreshToken();
        log.info("Token刷新请求");

        // 1. 验证 Refresh Token 的 JWT 签名和有效期
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            log.warn("Refresh Token无效或已过期");
            return JsonData.buildResult(BizCodeEnum.TOKEN_INVALID);
        }

        // 2. 从 Refresh Token 中解析用户ID
        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
        if (userId == null) {
            log.warn("Refresh Token解析用户ID失败");
            return JsonData.buildResult(BizCodeEnum.TOKEN_INVALID);
        }

        // 3. 校验 Redis 中存储的 Refresh Token 是否一致（防重放攻击）
        String storedToken = userDetailsService.getRefreshTokenByUserId(userId);
        if (storedToken == null) {
            // Redis 中不存在 Refresh Token，说明已过期或已被清除
            log.warn("Refresh Token在Redis中不存在，可能已过期: userId={}", userId);
            return JsonData.buildResult(BizCodeEnum.REFRESH_TOKEN_EXPIRED);
        }
        if (!storedToken.equals(refreshToken)) {
            // Token 已被使用过（Rotation 检测），可能存在盗用风险
            // 安全策略：清除该用户的所有登录状态，强制重新登录
            log.error("检测到Refresh Token重复使用，可能存在盗用风险: userId={}", userId);
            userDetailsService.removeRefreshToken(userId);
            return JsonData.buildResult(BizCodeEnum.TOKEN_REUSE_DETECTED);
        }

        // 4. 删除旧的 Refresh Token（实现一次性使用）
        userDetailsService.removeRefreshToken(userId);

        // 5. 查询用户信息，构建新的 LoginUser
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("刷新Token时用户不存在: userId={}", userId);
            return JsonData.buildResult(BizCodeEnum.USER_NOT_EXIST);
        }
        if (user.getStatus() == 0) {
            log.warn("刷新Token时账号已被禁用: userId={}", userId);
            return JsonData.buildResult(BizCodeEnum.ACCOUNT_DISABLED);
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setIsAdmin(user.getIsAdmin());
        loginUser.setPermissions(new HashSet<>());
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            loginUser.getPermissions().add("admin");
        }

        // 6. 生成新的 Access Token 和 Refresh Token
        String newAccessToken = jwtUtil.generateToken(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // 7. 将新 Token 存入 Redis
        userDetailsService.setLoginUser(newAccessToken, loginUser);
        userDetailsService.setRefreshToken(userId, newRefreshToken);
        // 更新用户当前登录的 Token（用于踢人机制）
        userDetailsService.setCurrentToken(userId, newAccessToken);

        // 8. 返回新 Token
        RefreshTokenVO vo = new RefreshTokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);

        log.info("Token刷新成功: userId={}", userId);
        return JsonData.buildSuccess(vo);
    }
    
    /**
     * 获取当前登录用户信息
     * 
     * 从 Spring Security 上下文中获取当前登录用户
     * 
     * @return 用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/info")
    public JsonData<UserInfoVO> getUserInfo() {
        // 1. 从 Spring Security 上下文获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = null;
        if (authentication != null) {
            loginUser = (LoginUser) authentication.getPrincipal();
        }

        // 2. 从数据库查询完整用户信息
        User user = null;
        if (loginUser != null) {
            user = userMapper.selectById(loginUser.getUserId());
        }

        // 3. 构建返回的用户信息
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setNickname(user.getNickname());
        userInfoVO.setAvatar(user.getAvatar());
        userInfoVO.setEmail(user.getEmail());
        userInfoVO.setPhone(user.getPhone());
        userInfoVO.setBio(user.getBio());
        userInfoVO.setIsAdmin(user.getIsAdmin());
        
        return JsonData.buildSuccess(userInfoVO);
    }
    
    /**
     * 用户登出
     * 
     * 登出流程由 LogoutSuccessHandlerImpl 处理
     * 这里只是一个空实现，实际的登出逻辑在 handler 中
     * 
     * @return 成功
     */
    @Operation(summary = "用户登出", description = "退出登录，清除登录状态")
    @PostMapping("/logout")
    public JsonData<Void> logout() {
        return JsonData.buildSuccess();
    }
}
