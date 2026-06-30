package com.personblog.admin.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 记录管理员操作日志注解
 *
 * @author LSH
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RecordLog {

    String Type() default "";

    String businessType() default "";

    String description() default "";
}
