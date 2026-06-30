package com.personblog.common.aspect.sensitive;

/**
 * 脱敏策略接口：每种敏感数据类型实现一个策略，只需关心如何脱敏
 * 字段名→策略的映射由 SensitiveAspect 统一注册管理
 */
public interface SensitiveStrategy {

    /**
     * 对字段值执行脱敏
     */
    String desensitize(String value);
}
