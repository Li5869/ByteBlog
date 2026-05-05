package com.personblog.security.filter;

import com.personblog.security.entity.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * API Key 认证过滤器
 * 
 * 用于内部服务间通信的万能 key 认证机制。
 * Python AI 服务通过请求头 X-API-Key 传递万能 key，
 * 验证通过后以系统身份访问受保护资源。
 * 
 * 认证流程：
 * 1. 从请求头 X-API-Key 提取 key
 * 2. 与配置的万能 key 比对
 * 3. 匹配成功则创建系统级认证上下文
 * 
 * @author LSH
 */
@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${api-key.header-name:X-API-Key}")
    private String apiKeyHeader;

    @Value("${api-key.universal-key:HSIL}")
    private String universalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String apiKey = resolveApiKey(request);

            if (StringUtils.hasText(apiKey)) {
                if (universalApiKey.equals(apiKey)) {
                    log.debug("API Key 认证通过，来源: {}", request.getRemoteAddr());

                    LoginUser systemUser = new LoginUser();
                    systemUser.setUserId(0L);
                    systemUser.setUsername("ai-agent");
                    systemUser.setPermissions(Set.of("ai:access"));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    systemUser,
                                    null,
                                    systemUser.getAuthorities()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("API Key 验证失败，来源: {}", request.getRemoteAddr());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            if (SecurityContextHolder.getContext().getAuthentication() != null
                    && "ai-agent".equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private String resolveApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader(apiKeyHeader);

        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }

        apiKey = request.getParameter("api_key");
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }

        return null;
    }
}
