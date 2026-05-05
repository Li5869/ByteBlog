package com.personblog.common.adminLog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 记录管理员操作日志注解
 * 标注在Controller方法上，自动记录操作日志
 *
 * @author LSH
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RecordLog {

    /**
     * 操作类型
     * 如：login、logout、create、update、delete、review、export、import等
     */
    String Type() default "";

    /**
     * 业务类型/操作对象类型
     * 如：article、user、comment、question、category、tag等
     */
    String businessType() default "";

    /**
     * 操作描述
     * 如：发布文章、删除用户、审核评论等
     */
    String description() default "";
}
