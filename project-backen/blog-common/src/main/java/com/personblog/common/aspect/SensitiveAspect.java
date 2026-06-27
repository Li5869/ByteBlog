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
import java.util.List;

/**
 * 方法级别脱敏切面：对Controller返回值中的敏感信息进行脱敏处理
 * 在Controller方法上标注 @SensitiveResponse 即可生效
 * 新增脱敏类型只需实现 SensitiveStrategy 并添加到 STRATEGIES 列表
 */
@Slf4j
@Aspect
@Component
public class SensitiveAspect {

    // 策略列表，新增脱敏类型在此注册即可
    private static final List<SensitiveStrategy> STRATEGIES = List.of(
            new PhoneSensitiveStrategy(),
            new EmailSensitiveStrategy()
    );

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
        // 跳过JDK类、基本类型、枚举等，只处理自定义业务对象
        if (obj.getClass().getName().startsWith("java.") || obj instanceof Enum) {
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
                    // 反射访问字段值
                    field.setAccessible(true);
                    String value = (String) field.get(obj);
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    // 通过字段名匹配策略，首个命中即脱敏
                    String desensitized = applyStrategies(field.getName(), value);
                    if (!desensitized.equals(value)) {
                        field.set(obj, desensitized);
                    }
                } catch (Exception e) {
                    // 反射访问失败不影响主流程
                    log.warn("脱敏处理字段失败: {}.{}", clazz.getSimpleName(), field.getName());
                }
            }
            // 继续处理父类字段
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 通过字段名匹配策略，首个命中即脱敏返回；无匹配则原值返回
     */
    private String applyStrategies(String fieldName, String value) {
        for (SensitiveStrategy strategy : STRATEGIES) {
            if (strategy.matchesField(fieldName)) {
                return strategy.desensitize(value);
            }
        }
        return value;
    }
}
