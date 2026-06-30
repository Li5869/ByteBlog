package com.personblog.coupon.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 优惠券系统 MQ 配置
 * 定义优惠券相关的交换机、队列、绑定及死信队列
 *
 * @author LSH
 * @since 2026-06-03
 */
@Configuration
public class CouponMqConfig {

    // ===== 队列名常量 =====

    /** 优惠券领取队列 */
    public static final String COUPON_CLAIM_QUEUE = "coupon_claim_queue";

    // ===== 交换机名 =====

    /** 优惠券业务交换机 */
    public static final String COUPON_EXCHANGE = "coupon_exchange";

    // ===== 路由键 =====

    /** 优惠券领取路由键 */
    public static final String COUPON_CLAIM_KEY = "coupon.claim.key";

    // ===== 死信交换机 & 死信队列 & 死信路由键 =====

    /** 优惠券死信交换机 */
    public static final String COUPON_DLX = "coupon.dlx";
    /** 优惠券领取死信队列 */
    public static final String COUPON_CLAIM_DLQ = "coupon_claim_dlq";
    /** 优惠券领取死信路由键 */
    public static final String COUPON_CLAIM_DLK = "coupon.claim.dlk";

    // ===== 业务队列 Bean（绑定死信） =====

    /**
     * 优惠券领取队列，消费失败转入 coupon_claim_dlq
     */
    @Bean
    public Queue couponClaimQueue() {
        return QueueBuilder.durable(COUPON_CLAIM_QUEUE)
                .withArgument("x-dead-letter-exchange", COUPON_DLX)
                .withArgument("x-dead-letter-routing-key", COUPON_CLAIM_DLK)
                .build();
    }

    // ===== 业务交换机 & 绑定 =====

    /**
     * 优惠券业务交换机（DirectExchange，持久化）
     */
    @Bean
    public DirectExchange couponExchange() {
        return new DirectExchange(COUPON_EXCHANGE, true, false);
    }

    /**
     * 优惠券领取队列绑定到业务交换机
     */
    @Bean
    public Binding couponClaimBinding() {
        return BindingBuilder.bind(couponClaimQueue()).to(couponExchange()).with(COUPON_CLAIM_KEY);
    }

    // ===== 死信交换机 & 死信队列 Bean & 绑定 =====

    /**
     * 优惠券死信交换机（DirectExchange，持久化）
     */
    @Bean
    public DirectExchange couponDlx() {
        return new DirectExchange(COUPON_DLX, true, false);
    }

    /**
     * 优惠券领取死信队列
     */
    @Bean
    public Queue couponClaimDlq() {
        return QueueBuilder.durable(COUPON_CLAIM_DLQ).build();
    }

    /**
     * 优惠券领取死信队列绑定到死信交换机
     */
    @Bean
    public Binding couponClaimDlqBinding() {
        return BindingBuilder.bind(couponClaimDlq()).to(couponDlx()).with(COUPON_CLAIM_DLK);
    }
}
