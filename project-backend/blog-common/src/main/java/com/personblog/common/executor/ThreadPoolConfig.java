package com.personblog.common.executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * <p>
 * 按业务场景划分线程池，参数根据任务特征差异化配置：
 * <ul>
 *   <li>普通任务（文章计数/评论/消息）：中等并发</li>
 *   <li>IO密集任务（AI调用）：较高并发，大队列</li>
 *   <li>特殊任务（死信队列重试）：低并发，小队列</li>
 *   <li>缓存重建任务：虚拟线程（JDK 21+）</li>
 * </ul>
 *
 * @author LSH
 */
@Configuration
public class ThreadPoolConfig {

    // ==================== 通用线程池创建 ====================

    /**
     * 创建带有统一配置的线程池
     *
     * @param namePrefix  线程名前缀，便于线上排查（如 "article-count-"）
     * @param coreSize    核心线程数
     * @param maxSize     最大线程数
     * @param queueCap    任务队列容量
     * @return 配置完成的线程池执行器
     */
    private ThreadPoolTaskExecutor createExecutor(String namePrefix, int coreSize, int maxSize, int queueCap) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCap);
        // 线程名前缀，日志中可直接看到线程归属
        executor.setThreadFactory(new CustomizableThreadFactory(namePrefix));
        // 关闭时等待队列中剩余任务执行完毕
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        // 拒绝策略：由调用线程执行，避免任务丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // ==================== 普通任务线程池（文章/评论/消息） ====================

    /**
     * 文章计数线程池（更新用户文章数、分类文章数等）
     */
    @Bean(name = "ArticleCountExecutor")
    public Executor articleCountExecutor() {
        return createExecutor("article-count-", 4, 8, 256);
    }

    /**
     * 评论处理线程池（评论计数更新等）
     */
    @Bean(name = "CommentExecutor")
    public Executor commentExecutor() {
        return createExecutor("comment-", 4, 8, 256);
    }

    /**
     * 消息处理线程池（私信/通知的异步处理）
     */
    @Bean(name = "MessageExecutor")
    public Executor messageExecutor() {
        return createExecutor("message-", 4, 8, 256);
    }

    // ==================== IO密集型线程池（AI调用） ====================

    /**
     * AI消息处理线程池（调用外部AI接口，IO等待时间长）
     * <p>
     * 核心线程数较高以应对AI接口的长耗时等待，避免请求堆积
     */
    @Bean(name = "AiMessageExecutor")
    public Executor aiMessageExecutor() {
        return createExecutor("ai-msg-", 8, 16, 512);
    }

    // ==================== 特殊线程池 ====================

    /**
     * 死信队列重试线程池（RabbitMQ DLQ 重试，低并发后台任务）
     */
    @Bean(name = "DlqExecutor")
    public Executor dlqExecutor() {
        return createExecutor("dlq-retry-", 2, 4, 64);
    }

    // ==================== 优惠券计算线程池 ====================

    /**
     * 优惠券最优计算线程池（并行计算优惠金额，CPU密集型，小线程池即可）
     */
    @Bean(name = "CouponCalcExecutor")
    public Executor couponCalcExecutor() {
        return createExecutor("coupon-calc-", 2, 4, 64);
    }

    // ==================== 缓存重建虚拟线程执行器 ====================

    /**
     * 缓存重建虚拟线程执行器（JDK 21+）
     * <p>
     * 用于多级缓存逻辑过期方案的异步重建任务：
     * <ul>
     *   <li>每个重建任务独立虚拟线程，轻量无阻塞</li>
     *   <li>适合 I/O 密集型任务（回源 DB + Redis）</li>
     *   <li>无线程数限制，可并发处理大量热点 key</li>
     * </ul>
     */
    @Bean(name = "CacheRebuildExecutor")
    public Executor cacheRebuildExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}