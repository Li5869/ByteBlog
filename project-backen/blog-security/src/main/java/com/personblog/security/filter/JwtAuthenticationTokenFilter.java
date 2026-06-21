package com.personblog.security.filter;

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
        
        // 1. 从请求头中提取 Token
        String token = resolveToken(request);

        // 2. 如果 Token 存在且有效，进行认证
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {

            // 2.1 从 Token 中解析用户ID
            Long userId = jwtUtil.getUserIdFromToken(token);
            log.debug("JWT Token 验证通过，用户ID: {}", userId);

            // 2.2 验证 Token 有效性并获取用户信息（比对 Redis 中存储的 token）
            LoginUser loginUser = userDetailsService.getLoginUser(userId, token);
            if (loginUser != null) {
                // 2.3 刷新 Token 过期时间（活跃用户不过期）
                userDetailsService.refreshToken(userId);

                // 2.4 创建认证对象（UserContextHolder 通过 SecurityContextHolder 自动获取用户信息）
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        loginUser,
                        null,
                        loginUser.getAuthorities()
                    );

                // 2.5 设置认证详情（包含请求信息）
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 2.6 将认证信息存入 SecurityContext（UserContextHolder 自动从中读取用户 ID）
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("用户认证成功: {}", loginUser.getUsername());
            } else {
                log.debug("Redis 中未找到登录信息，Token 可能已失效");
            }
        }

        // 3. 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 允许异步调度时重新执行过滤器
     * 控制器返回 Mono 等异步类型时，Spring MVC 会触发异步调度（DispatcherType.ASYNC），
     * OncePerRequestFilter 默认跳过异步调度，导致安全上下文无法传播到新线程，
     * 从而触发 AuthenticationEntryPoint 返回 401。
     * 返回 false 确保异步调度时也执行过滤，重新验证 Token 并设置安全上下文。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
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
