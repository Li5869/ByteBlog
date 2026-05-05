package com.personblog.security.handler;

import com.alibaba.fastjson2.JSON;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.result.JsonData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 认证失败处理器
 * 
 * 实现 AuthenticationEntryPoint 接口
 * 当未认证用户访问受保护资源时，Spring Security 会调用此处理器
 * 
 * 触发场景：
 * 1. 未携带 Token 访问需要认证的接口
 * 2. Token 无效或已过期
 * 3. Token 被篡改
 * 
 * 返回 401 状态码和错误信息
 * 
 * @author LSH
 */
@Slf4j
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    
    /**
     * 处理认证失败
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param authException 认证异常
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response, 
                        AuthenticationException authException) throws IOException {
        
        // 记录日志
        log.warn("认证失败: {} - {}", request.getRequestURI(), authException.getMessage());
        
        // 设置响应类型和编码
        response.setContentType("application/json;charset=UTF-8");
        // 设置 HTTP 状态码为 401 未授权
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // 构建错误响应
        JsonData<Void> result = JsonData.buildResult(BizCodeEnum.NOT_LOGIN);
        
        // 写入响应
        response.getWriter().write(JSON.toJSONString(result));
    }
}
