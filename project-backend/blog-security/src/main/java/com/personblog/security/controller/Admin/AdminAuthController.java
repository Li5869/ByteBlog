package com.personblog.security.controller.Admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.admin.aspect.RecordLog;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.result.JsonData;
import com.personblog.security.dto.LoginDTO;
import com.personblog.security.dto.RefreshTokenDTO;
import com.personblog.security.entity.LoginUser;
import com.personblog.security.entity.User;
import com.personblog.security.mapper.UserMapper;
import com.personblog.security.service.UserDetailsServiceImpl;
import com.personblog.security.utils.JwtUtil;
import com.personblog.security.vo.AdminInfoVO;
import com.personblog.security.vo.RefreshTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * 管理端认证控制器
 *
 * 独立于用户端的管理员认证接口，仅允许管理员登录
 *
 * @author LSH
 */
@Slf4j
@Tag(name = "管理端-认证管理", description = "管理员登录、登出、刷新Token等接口")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 管理员登录
     *
     * 与用户端登录接口独立，仅允许管理员账号登录
     * 非管理员账号将直接拒绝
     */
    @Operation(summary = "管理员登录", description = "管理员专用登录接口，非管理员账号无法登录")
    @PostMapping("/login")
    @RecordLog(Type = "login",description = "登陆")
    public JsonData<AdminInfoVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("管理员登录请求: {}", loginDTO.getUsername());

        try {
            // 1. 先检查用户状态和管理员身份（避免 Spring Security 异常包装）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, loginDTO.getUsername())
                    .or()
                    .eq(User::getEmail, loginDTO.getUsername())
                    .or()
                    .eq(User::getPhone, loginDTO.getUsername());
            User checkUser = userMapper.selectOne(queryWrapper);

            if (checkUser == null) {
                log.warn("管理员登录失败 - 用户不存在: {}", loginDTO.getUsername());
                return JsonData.buildResult(BizCodeEnum.LOGIN_ERROR);
            }
            if (checkUser.getStatus() == 0) {
                log.warn("管理员登录失败 - 账号已被封禁: {}", loginDTO.getUsername());
                return JsonData.buildResult(BizCodeEnum.ACCOUNT_DISABLED);
            }
            if (!Boolean.TRUE.equals(checkUser.getIsAdmin())) {
                log.warn("管理员登录失败 - 非管理员账号: {}", loginDTO.getUsername());
                return JsonData.buildResult(BizCodeEnum.NO_POWER);
            }

            // 2. 认证
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            // 3. 生成 Token
            String token = jwtUtil.generateToken(loginUser.getUserId());
            String refreshToken = jwtUtil.generateRefreshToken(loginUser.getUserId());

            // 4. 存入 Redis
            userDetailsService.setLoginUser(token, loginUser);
            userDetailsService.setRefreshToken(loginUser.getUserId(), refreshToken);

            // 5. 更新最后登录时间
            User updateUser = new User();
            updateUser.setId(loginUser.getUserId());
            updateUser.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(updateUser);

            // 6. 构建返回
            AdminInfoVO vo = new AdminInfoVO();
            vo.setId(loginUser.getUserId());
            vo.setUsername(loginUser.getUsername());
            vo.setNickname(checkUser.getNickname());
            vo.setAvatar(checkUser.getAvatar());
            vo.setToken(token);
            vo.setRefreshToken(refreshToken);

            log.info("管理员登录成功: userId={}, username={}", loginUser.getUserId(), loginUser.getUsername());
            return JsonData.buildSuccess(vo);

        } catch (BadCredentialsException e) {
            log.warn("管理员登录失败 - 用户名或密码错误: {}", loginDTO.getUsername());
            return JsonData.buildResult(BizCodeEnum.LOGIN_ERROR);
        } catch (Exception e) {
            log.error("管理员登录失败 - 系统错误: {}", e.getMessage(), e);
            return JsonData.buildResult(BizCodeEnum.LOGIN_ERROR);
        }
    }

    /**
     * 获取当前管理员信息
     */
    @Operation(summary = "获取管理员信息", description = "获取当前登录管理员的详细信息")
    @GetMapping("/info")
    public JsonData<AdminInfoVO> getAdminInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = null;
        if (authentication != null) {
            loginUser = (LoginUser) authentication.getPrincipal();
        }

        User user = null;
        if (loginUser != null) {
            user = userMapper.selectById(loginUser.getUserId());
        }

        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return JsonData.buildSuccess(vo);
    }

    /**
     * 刷新管理员 Token
     *
     * 使用 Refresh Token 获取新的 Access Token（一次性刷新机制）
     */
    @Operation(summary = "刷新Token", description = "使用 Refresh Token 获取新的 Access Token")
    @PostMapping("/refresh")
    public JsonData<RefreshTokenVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        String refreshToken = dto.getRefreshToken();
        log.info("管理员Token刷新请求");

        // 1. 验证 Refresh Token
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            log.warn("Refresh Token无效或已过期");
            return JsonData.buildResult(BizCodeEnum.TOKEN_INVALID);
        }

        // 2. 解析用户ID
        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
        if (userId == null) {
            log.warn("Refresh Token解析用户ID失败");
            return JsonData.buildResult(BizCodeEnum.TOKEN_INVALID);
        }

        // 3. 校验 Redis 中的 Refresh Token
        String storedToken = userDetailsService.getRefreshTokenByUserId(userId);
        if (storedToken == null) {
            log.warn("Refresh Token在Redis中不存在: userId={}", userId);
            return JsonData.buildResult(BizCodeEnum.REFRESH_TOKEN_EXPIRED);
        }
        if (!storedToken.equals(refreshToken)) {
            log.error("检测到Refresh Token重复使用: userId={}", userId);
            userDetailsService.removeRefreshToken(userId);
            return JsonData.buildResult(BizCodeEnum.TOKEN_REUSE_DETECTED);
        }

        // 4. 删除旧的 Refresh Token
        userDetailsService.removeRefreshToken(userId);

        // 5. 查询用户，校验管理员身份
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("刷新Token时用户不存在: userId={}", userId);
            return JsonData.buildResult(BizCodeEnum.USER_NOT_EXIST);
        }
        if (user.getStatus() == 0) {
            log.warn("刷新Token时账号已被禁用: userId={}", userId);
            return JsonData.buildResult(BizCodeEnum.ACCOUNT_DISABLED);
        }
        if (!Boolean.TRUE.equals(user.getIsAdmin())) {
            log.warn("刷新Token时非管理员账号: userId={}", userId);
            return JsonData.buildResult(BizCodeEnum.NO_POWER);
        }

        // 6. 构建 LoginUser
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setIsAdmin(true);
        loginUser.setPermissions(new HashSet<>());
        loginUser.getPermissions().add("admin");

        // 7. 生成新 Token
        String newAccessToken = jwtUtil.generateToken(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // 8. 存入 Redis
        userDetailsService.setLoginUser(newAccessToken, loginUser);
        userDetailsService.setRefreshToken(userId, newRefreshToken);

        // 9. 返回
        RefreshTokenVO vo = new RefreshTokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);

        log.info("管理员Token刷新成功: userId={}", userId);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 管理员登出
     */
    @Operation(summary = "管理员登出", description = "退出管理后台，清除登录状态")
    @PostMapping("/logout")
    @RecordLog(Type = "logout",description = "登出")
    public JsonData<Void> logout() {
        return JsonData.buildSuccess();
    }
}
