package com.personblog.security.handler;

import com.alibaba.fastjson2.JSON;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.result.JsonData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 授权失败处理器
 * 
 * 实现 AccessDeniedHandler 接口
 * 当已认证用户访问无权限的资源时，Spring Security 会调用此处理器
 * 
 * 触发场景：
 * 1. 普通用户访问管理员接口（/admin/**）
 * 2. 用户访问需要特定权限的资源
 * 3. @PreAuthorize 注解校验失败
 * 
 * 返回 403 状态码和错误信息
 * 
 * @author LSH
 */
@Slf4j
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    
    /**
     * 处理授权失败
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param accessDeniedException 授权异常
     */
    @Override
    public void handle(HttpServletRequest request, 
                      HttpServletResponse response, 
                      AccessDeniedException accessDeniedException) throws IOException {
        
        // 记录日志
        log.warn("授权失败: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());
        
        // 设置响应类型和编码
        response.setContentType("application/json;charset=UTF-8");
        // 设置 HTTP 状态码为 403 禁止访问
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        // 构建错误响应
        JsonData<Void> result = JsonData.buildResult(BizCodeEnum.NO_POWER);
        
        // 写入响应
        response.getWriter().write(JSON.toJSONString(result));
    }
}
