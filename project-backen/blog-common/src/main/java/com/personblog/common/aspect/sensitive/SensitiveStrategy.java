package com.personblog.common.aspect.sensitive;

/**
 * 脱敏策略接口：每种敏感数据类型实现一个策略
 * 新增脱敏类型只需实现此接口并注册到 SensitiveAspect 即可
 */
public interface SensitiveStrategy {

    /**
     * 通过字段名判断是否需要当前策略脱敏
     * 如字段名含 phone/mobile/tel 则匹配手机号策略
     */
    boolean matchesField(String fieldName);

    /**
     * 对字段值执行脱敏
     */
    String desensitize(String value);
}
