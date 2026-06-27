package com.personblog.common.aspect.sensitive;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 手机号脱敏策略：13812345678 → 138****5678
 * 通过字段名含 phone 判断是否脱敏
 */
public class PhoneSensitiveStrategy implements SensitiveStrategy {

    // 匹配手机号的字段名关键词
    private static final Set<String> FIELD_KEYWORDS = Set.of("phone");
    // 脱敏正则
    private static final Pattern PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

    @Override
    public boolean matchesField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return FIELD_KEYWORDS.stream().anyMatch(lower::contains);
    }

    @Override
    public String desensitize(String value) {
        return PATTERN.matcher(value).replaceAll("$1****$2");
    }
}
