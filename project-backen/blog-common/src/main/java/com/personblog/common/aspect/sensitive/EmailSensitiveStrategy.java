package com.personblog.common.aspect.sensitive;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 邮箱脱敏策略：testname@example.com → tes***@example.com
 * 通过字段名含 email 判断是否脱敏
 */
public class EmailSensitiveStrategy implements SensitiveStrategy {

    // 匹配邮箱的字段名关键词
    private static final Set<String> FIELD_KEYWORDS = Set.of("email");
    // 脱敏正则：保留前3位，其余用***替换
    private static final Pattern PATTERN = Pattern.compile("(\\w{1,3})\\w+(@\\w+\\.\\w+)");

    @Override
    public boolean matchesField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return FIELD_KEYWORDS.stream().anyMatch(lower::contains);
    }

    @Override
    public String desensitize(String value) {
        return PATTERN.matcher(value).replaceAll("$1***$2");
    }
}
