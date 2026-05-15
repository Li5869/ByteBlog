package com.personblog.push.websocket;

import com.personblog.common.api.TokenValidationApi;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 验证 Token 并提取用户信息
 *
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketInterceptor implements HandshakeInterceptor {

    private final TokenValidationApi tokenValidationApi;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            String token = httpRequest.getParameter("token");
            if (token == null || token.isEmpty()) {
                log.warn("[WebSocket] 握手失败: 缺少 token 参数");
                return false;
            }

            // 验证 token
            if (!tokenValidationApi.validateToken(token)) {
                log.warn("[WebSocket] 握手失败: token 无效或已过期");
                return false;
            }

            // 从 token 中获取用户ID
            Long userId = tokenValidationApi.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("[WebSocket] 握手失败: 无法从 token 中解析用户ID");
                return false;
            }

            // 将用户信息放入 attributes
            attributes.put("userId", userId);
            attributes.put("token", token);
            attributes.put("loginTime", System.currentTimeMillis());
            return true;
        }

        log.warn("[WebSocket] 握手失败: 请求类型不正确");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("[WebSocket] 握手异常: {}", exception.getMessage());
        }
    }
}
