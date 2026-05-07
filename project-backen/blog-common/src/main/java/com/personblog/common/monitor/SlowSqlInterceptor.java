package com.personblog.common.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.stereotype.Component;

import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * 慢 SQL 拦截器
 *
 * 拦截 MyBatis 的 StatementHandler.query/update 方法，
 * 记录执行耗时，超过阈值输出完整 SQL 到日志。
 * 同时注册 Micrometer 指标，可在 Prometheus/Grafana 中观测。
 *
 * @author LSH
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
public class SlowSqlInterceptor implements Interceptor {

    /** 慢 SQL 阈值（毫秒） */
    private static final long SLOW_SQL_THRESHOLD_MS = 200;

    private final Timer sqlTimer;

    public SlowSqlInterceptor(MeterRegistry meterRegistry) {
        this.sqlTimer = Timer.builder("mybatis.sql.execution.duration")
                .description("SQL 执行耗时")
                .register(meterRegistry);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long durationNanos = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

            // 记录到 Micrometer
            sqlTimer.record(durationNanos, TimeUnit.NANOSECONDS);

            // 超过阈值输出慢 SQL 日志
            if (durationMs > SLOW_SQL_THRESHOLD_MS) {
                StatementHandler handler = (StatementHandler) invocation.getTarget();
                BoundSql boundSql = handler.getBoundSql();
                String sql = boundSql.getSql().replaceAll("[\\s]+", " ");

                // 尝试获取 MappedStatement 的 ID（如 mapper 方法全路径）
                String mapperId = getMappedStatementId(handler);

                log.warn("慢SQL: {}ms | mapper={} | SQL={}", durationMs, mapperId, sql);
            }
        }
    }

    /**
     * 从 StatementHandler 中提取 MappedStatement 的 ID
     */
    private String getMappedStatementId(StatementHandler handler) {
        try {
            MetaObject metaObject = SystemMetaObject.forObject(handler);
            MappedStatement ms = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
            return ms != null ? ms.getId() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
