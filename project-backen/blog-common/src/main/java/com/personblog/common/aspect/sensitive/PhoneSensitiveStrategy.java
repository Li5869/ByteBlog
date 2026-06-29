package com.personblog.common.aspect.sensitive;

import java.util.regex.Pattern;

/**
 * 手机号脱敏策略：13812345678 → 138****5678
 */
public class PhoneSensitiveStrategy implements SensitiveStrategy {

    private static final Pattern PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

    @Override
    public String desensitize(String value) {
        return PATTERN.matcher(value).replaceAll("$1****$2");
    }
}
