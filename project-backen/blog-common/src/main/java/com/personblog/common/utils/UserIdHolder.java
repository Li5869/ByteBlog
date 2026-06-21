package com.personblog.common.utils;

/**
 * 用户 ID 持有者接口
 * <p>
 * 用于解耦 blog-common 和 blog-security 模块。
 * blog-security 中的 {@code LoginUser} 实现此接口，
 * blog-common 中的 {@code UserContextHolder} 通过此接口获取用户 ID，
 * 从而避免直接依赖 Spring Security 的 SecurityContextHolder。
 *
 * @author LSH
 */
public interface UserIdHolder {

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID，未登录时返回 null
     */
    Long getUserId();
}
