package com.personblog.common.aspect;

import com.personblog.common.anno.SensitiveResponse;
import com.personblog.common.aspect.sensitive.EmailSensitiveStrategy;
import com.personblog.common.aspect.sensitive.PhoneSensitiveStrategy;
import com.personblog.common.aspect.sensitive.SensitiveStrategy;
import com.personblog.common.result.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 方法级别脱敏切面：对Controller返回值中的敏感信息进行脱敏处理
 * 在Controller方法上标注 @SensitiveResponse 即可生效
 * 新增脱敏类型只需实现 SensitiveStrategy 并在 STRATEGY_MAP 中注册关键词映射
 */
@Slf4j
@Aspect
@Component
public class SensitiveAspect {

    // 关键词→策略映射，字段名含关键词即命中对应策略，新增类型在此注册
    private static final Map<String, SensitiveStrategy> STRATEGY_MAP = new HashMap<>();

    static {
        STRATEGY_MAP.put("phone", new PhoneSensitiveStrategy());
        STRATEGY_MAP.put("email", new EmailSensitiveStrategy());
    }

    @Around("@annotation(response)")
    public Object sensitiveResponse(ProceedingJoinPoint joinPoint, SensitiveResponse response) throws Throwable {
        Object proceed = joinPoint.proceed();
        // 仅处理 JsonData 包装的返回值
        if (proceed instanceof JsonData<?> jsonData && jsonData.getData() != null) {
            processObject(jsonData.getData());
        }
        return proceed;
    }

    /**
     * 递归处理对象的所有String字段，通过字段名匹配策略进行脱敏
     */
    private void processObject(Object obj) {
        if (obj == null) {
            return;
        }
        // 集合类型：遍历每个元素递归处理
        if (obj instanceof Collection<?> collection) {
            collection.forEach(this::processObject);
            return;
        }
        // 跳过枚举，JDK/框架类由 while 循环的 com. 前缀过滤
        if (obj instanceof Enum) {
            return;
        }

        // 遍历当前类及父类的所有字段
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz.getName().startsWith("com.")) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() != String.class) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    String value = (String) field.get(obj);
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    // 通过字段名从策略工厂查找策略
                    SensitiveStrategy strategy = matchStrategy(field.getName());
                    if (strategy != null) {
                        field.set(obj, strategy.desensitize(value));
                    }
                } catch (Exception e) {
                    // 反射访问失败不影响主流程
                    log.warn("脱敏处理字段失败: {}.{}", clazz.getSimpleName(), field.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 字段名含某关键词则返回对应策略，否则返回null
     */
    private SensitiveStrategy matchStrategy(String fieldName) {
        String lower = fieldName.toLowerCase();
        for (Map.Entry<String, SensitiveStrategy> entry : STRATEGY_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
