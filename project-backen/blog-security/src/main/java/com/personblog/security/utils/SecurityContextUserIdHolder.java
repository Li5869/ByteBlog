package com.personblog.security.utils;

import com.personblog.common.service.UserIdHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 从 Spring Security 上下文中获取用户 ID 的实现
 * <p>
 * 作为 UserIdHolder 的 Spring Bean 注入到 UserContextHolder，
 * 使业务代码通过 UserContextHolder.getUserId() 即可获取当前用户，
 * 无需直接依赖 SecurityContextHolder。
 *
 * @author LSH
 */
@Component
public class SecurityContextUserIdHolder implements UserIdHolder {

    @Override
    public Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // principal 是 LoginUser（实现了 UserIdHolder），直接提取 userId
        if (principal instanceof UserIdHolder holder) {
            return holder.getUserId();
        }
        return null;
    }
}
