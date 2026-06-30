package com.personblog.common.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务指标注册器
 *
 * 注册自定义业务指标到 Micrometer，暴露给 Prometheus
 * 包含：文章发布计数、用户注册计数、评论提交计数、AI 调用计数及耗时
 *
 * @author LSH
 */
@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    private Counter articlePublishCounter;
    private Counter userRegisterCounter;
    private Counter commentSubmitCounter;
    private Counter aiCallCounter;
    private Timer aiCallTimer;

    @PostConstruct
    public void init() {
        articlePublishCounter = Counter.builder("business.article.publish")
                .description("文章发布次数")
                .register(meterRegistry);

        userRegisterCounter = Counter.builder("business.user.register")
                .description("用户注册次数")
                .register(meterRegistry);

        commentSubmitCounter = Counter.builder("business.comment.submit")
                .description("评论提交次数")
                .register(meterRegistry);

        aiCallCounter = Counter.builder("business.ai.call")
                .description("AI 接口调用次数")
                .register(meterRegistry);

        aiCallTimer = Timer.builder("business.ai.call.duration")
                .description("AI 接口调用耗时")
                .register(meterRegistry);
    }

    public void recordArticlePublish() {
        articlePublishCounter.increment();
    }

    public void recordUserRegister() {
        userRegisterCounter.increment();
    }

    public void recordCommentSubmit() {
        commentSubmitCounter.increment();
    }

    public void recordAiCall() {
        aiCallCounter.increment();
    }

    /**
     * 启动 AI 调用计时器
     * 使用方式：Timer.Sample sample = businessMetrics.startAiCallTimer();
     */
    public Timer.Sample startAiCallTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 停止 AI 调用计时器并记录耗时
     * 使用方式：businessMetrics.stopAiCallTimer(sample);
     */
    public void stopAiCallTimer(Timer.Sample sample) {
        sample.stop(aiCallTimer);
    }
}
