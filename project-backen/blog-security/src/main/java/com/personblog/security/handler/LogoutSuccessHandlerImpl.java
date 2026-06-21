package com.personblog.security.handler;

import com.alibaba.fastjson2.JSON;
import com.personblog.common.result.JsonData;
import com.personblog.security.entity.LoginUser;
import com.personblog.security.service.UserDetailsServiceImpl;
import com.personblog.security.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 登出成功处理器
 * 
 * 实现 LogoutSuccessHandler 接口
 * 当用户请求登出时，Spring Security 会调用此处理器
 * 
 * 登出流程：
 * 1. 从请求头获取 Token
 * 2. 从 Redis 删除登录信息（Access Token 和 Refresh Token）
 * 3. 返回成功响应
 * 
 * 注意：登出后 Token 虽然在 JWT 层面仍然有效
 * 但由于 Redis 中已删除登录信息，该 Token 将无法再使用
 * 
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {
    
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    
    /**
     * 处理登出成功
     * 
     * 同时清除 Access Token 和 Refresh Token
     * 确保用户完全登出，无法再通过任何 Token 访问系统
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param authentication 认证信息（可能为 null）
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Authentication authentication) throws IOException {
        
        // 1. 从请求头获取 Token（统一使用 "token" 头）
        String token = request.getHeader("token");
        
        if (token != null && !token.isEmpty()) {
            // 2. 从 Token 中解析 userId
            Long userId = jwtUtil.getUserIdFromToken(token);

            if (userId != null) {
                // 3. 删除登录 Token（user:login:{userId}）和 Refresh Token
                userDetailsService.removeLoginUser(userId);
                userDetailsService.removeRefreshToken(userId);
                log.info("用户登出成功，已清除 Access Token 和 Refresh Token: userId={}", userId);
            } else {
                // Token 解析失败，尝试从 authentication 获取
                if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                    userDetailsService.removeLoginUser(loginUser.getUserId());
                    userDetailsService.removeRefreshToken(loginUser.getUserId());
                    log.info("用户登出成功，已清除 Access Token 和 Refresh Token: userId={}", loginUser.getUserId());
                } else {
                    log.warn("用户登出成功，但无法获取 userId，Token 可能未被清除");
                }
            }
        }
        
        // 4. 设置响应类型和编码
        response.setContentType("application/json;charset=UTF-8");
        
        // 5. 构建成功响应
        JsonData<Void> result = JsonData.buildSuccess();
        
        // 6. 写入响应
        response.getWriter().write(JSON.toJSONString(result));
    }
}
