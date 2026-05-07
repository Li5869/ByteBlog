package com.personblog.common.monitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 慢请求日志过滤器
 *
 * 记录耗时超过阈值的 HTTP 请求，便于排查性能瓶颈
 * 优先级设为最高，确保在其他过滤器之前开始计时
 *
 * @author LSH
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SlowRequestFilter extends OncePerRequestFilter {

    /** 慢请求阈值（毫秒） */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("慢请求: {} {} | 耗时: {}ms | 状态码: {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        duration,
                        response.getStatus());
            }
        }
    }
}
