package com.personblog.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户上下文持有者
 * <p>
 * 通过静态方法对外暴露获取当前登录用户 ID 的能力，
 * 内部委托给 Spring 容器中的 {@link UserIdHolder} 实现（即 SecurityContextUserIdHolder）。
 * <p>
 * 业务代码只需调用 {@code UserContextHolder.getUserId()} 即可获取当前用户，
 * 无需直接依赖 Spring Security 的 SecurityContextHolder。
 *
 * @author LSH
 */
@Component
public class UserContextHolder {

    private static UserIdHolder userIdHolder;

    /**
     * 通过 setter 注入 UserIdHolder 实现，赋值给静态字段
     * 这是 Spring 注入静态字段的标准做法
     */
    @Autowired
    public void setUserIdHolder(UserIdHolder holder) {
        UserContextHolder.userIdHolder = holder;
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID，未登录时返回 null
     */
    public static Long getUserId() {
        return userIdHolder == null ? null : userIdHolder.getUserId();
    }
}
