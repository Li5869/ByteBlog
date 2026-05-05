package com.personblog.security.filter;

import com.personblog.common.utils.UserContextHolder;
import com.personblog.security.config.JwtProperties;
import com.personblog.security.entity.LoginUser;
import com.personblog.security.service.UserDetailsServiceImpl;
import com.personblog.security.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 
 * 继承 OncePerRequestFilter，确保每个请求只过滤一次
 * 负责从请求头中提取 Token，验证并设置用户认证信息
 * 
 * 认证流程：
 * 1. 从请求头 Authorization 中提取 Bearer Token
 * 2. 验证 Token 有效性（签名、过期时间）
 * 3. 从 Redis 获取登录用户信息
 * 4. 将用户信息设置到 SecurityContext
 * 5. 刷新 Redis 中 Token 的过期时间
 * 
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserDetailsServiceImpl userDetailsService;
    
    /**
     * 过滤器核心方法
     * 
     * 每个请求都会经过此方法
     * 如果 Token 有效，将用户信息设置到 SecurityContext
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. 从请求头中提取 Token
            String token = resolveToken(request);
            
            // 2. 如果 Token 存在且有效，进行认证
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                
                // 2.1 从 Token 中解析用户ID
                Long userId = jwtUtil.getUserIdFromToken(token);
                log.debug("JWT Token 验证通过，用户ID: {}", userId);
                
                // 2.2 从 Redis 获取登录用户信息
                // 注意：Token 有效不代表 Redis 中有登录信息（可能已登出）
                LoginUser loginUser = userDetailsService.getLoginUserByToken(token);
                if (loginUser != null) {
                    // 2.3 刷新 Token 过期时间（活跃用户不过期）
                    userDetailsService.refreshToken(token);
                    
                    // 2.4 设置用户上下文（供业务代码使用）
                    UserContextHolder.setUserId(loginUser.getUserId());
                    
                    // 2.5 创建认证对象
                    // UsernamePasswordAuthenticationToken 是 Spring Security 的标准认证对象
                    // 参数：用户信息、凭证（null）、权限集合
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            loginUser,           // principal: 用户信息
                            null,                // credentials: 凭证（JWT 无状态，不需要）
                            loginUser.getAuthorities()  // authorities: 权限集合
                        );
                    
                    // 2.6 设置认证详情（包含请求信息）
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 2.7 将认证信息存入 SecurityContext
                    // 后续可以通过 SecurityContextHolder.getContext().getAuthentication() 获取
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("用户认证成功: {}", loginUser.getUsername());
                } else {
                    // Redis 中没有登录信息，说明用户已登出或 Token 已过期
                    log.debug("Redis 中未找到登录信息，Token 可能已失效");
                }
            }
            
            // 3. 继续过滤器链
            // 无论认证是否成功，都继续执行后续过滤器和 Controller
            // 未认证的请求会被 SecurityConfig 中的配置拦截
            filterChain.doFilter(request, response);
        } finally {
            // 4. 清理 ThreadLocal，避免内存泄漏
            UserContextHolder.clearUserId();
        }
    }
    
    /**
     * 从请求头或查询参数中提取 Token
     * 
     * 优先从请求头 token 获取，其次从查询参数 token 获取（用于 SSE 等无法设置请求头的场景）
     * 
     * @param request HTTP 请求
     * @return Token 字符串，不存在则返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        // 优先从 token 请求头获取
        String token = request.getHeader("token");
        
        if (StringUtils.hasText(token)) {
            return token;
        }
        
        // 从查询参数获取（用于 SSE 等无法设置请求头的场景）
        token = request.getParameter("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        
        return null;
    }
}
