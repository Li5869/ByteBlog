package com.personblog.vip.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * VIP 订单 MQ 配置
 * 定义订单相关的交换机、队列、绑定及死信队列
 *
 * @author LSH
 * @since 2026-06-07
 */
@Configuration
public class VipOrderMqConfig {

    // ===== 队列名常量 =====

    /** 订单超时延迟队列（消息 TTL 后转入超时消费队列） */
    public static final String ORDER_DELAY_QUEUE = "vip_order_delay_queue";
    /** 订单超时消费队列 */
    public static final String ORDER_TIMEOUT_QUEUE = "vip_order_timeout_queue";
    /** 订单确认队列（支付成功后激活会员） */
    public static final String ORDER_CONFIRM_QUEUE = "vip_order_confirm_queue";

    // ===== 交换机名 =====

    /** VIP 订单业务交换机 */
    public static final String VIP_ORDER_EXCHANGE = "vip_order_exchange";

    // ===== 路由键 =====

    /** 订单延迟路由键 */
    public static final String ORDER_DELAY_KEY = "vip.order.delay.key";
    /** 订单超时路由键 */
    public static final String ORDER_TIMEOUT_KEY = "vip.order.timeout.key";
    /** 订单确认路由键 */
    public static final String ORDER_CONFIRM_KEY = "vip.order.confirm.key";

    // ===== 死信交换机 & 死信队列 & 死信路由键 =====

    /** VIP 订单死信交换机 */
    public static final String VIP_ORDER_DLX = "vip_order.dlx";
    /** 订单超时死信队列 */
    public static final String ORDER_TIMEOUT_DLQ = "vip_order_timeout_dlq";
    /** 订单确认死信队列 */
    public static final String ORDER_CONFIRM_DLQ = "vip_order_confirm_dlq";
    /** 订单超时死信路由键 */
    public static final String ORDER_TIMEOUT_DLK = "vip.order.timeout.dlk";
    /** 订单确认死信路由键 */
    public static final String ORDER_CONFIRM_DLK = "vip.order.confirm.dlk";

    /** TCC Confirm 重试队列（指数退避重试积分扣减/优惠券核销） */
    public static final String TCC_CONFIRM_RETRY_QUEUE = "vip_tcc_confirm_retry_queue";
    /** TCC Confirm 重试延迟队列（无消费者，仅靠 TTL 过期后 DLX 回到重试队列实现退避） */
    public static final String TCC_CONFIRM_RETRY_DELAY_QUEUE = "vip_tcc_confirm_retry_delay_queue";
    /** TCC Confirm 重试死信队列（重试耗尽后人工处理） */
    public static final String TCC_CONFIRM_RETRY_DLQ = "vip_tcc_confirm_retry_dlq";
    /** TCC Confirm 重试路由键 */
    public static final String TCC_CONFIRM_RETRY_KEY = "vip.tcc.confirm.retry.key";
    /** TCC Confirm 重试延迟路由键 */
    public static final String TCC_CONFIRM_RETRY_DELAY_KEY = "vip.tcc.confirm.retry.delay.key";
    /** TCC Confirm 重试死信路由键 */
    public static final String TCC_CONFIRM_RETRY_DLK = "vip.tcc.confirm.retry.dlk";

    /** TCC Confirm 最大重试次数 */
    public static final int TCC_CONFIRM_MAX_RETRIES = 3;
    /** TCC Confirm 重试基础延迟（毫秒） */
    public static final long TCC_CONFIRM_BASE_DELAY_MS = 1000L;

    // ===== 订单超时时间（毫秒） =====

    public static final long ORDER_TIMEOUT_MS = 15 * 60 * 1000L;

    // ===== 业务队列 Bean =====

    /**
     * 订单超时延迟队列：消息 TTL 15分钟后自动转入超时消费队列
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", ORDER_TIMEOUT_MS);
        args.put("x-dead-letter-exchange", VIP_ORDER_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_TIMEOUT_KEY);
        return QueueBuilder.durable(ORDER_DELAY_QUEUE).withArguments(args).build();
    }

    /**
     * 订单超时消费队列，消费失败转入死信
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", VIP_ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_DLK)
                .build();
    }

    /**
     * 订单确认队列（支付成功后激活会员），消费失败转入死信
     */
    @Bean
    public Queue orderConfirmQueue() {
        return QueueBuilder.durable(ORDER_CONFIRM_QUEUE)
                .withArgument("x-dead-letter-exchange", VIP_ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_CONFIRM_DLK)
                .build();
    }

    // ===== 业务交换机 & 绑定 =====

    /**
     * VIP 订单业务交换机（DirectExchange，持久化）
     */
    @Bean
    public DirectExchange vipOrderExchange() {
        return new DirectExchange(VIP_ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(vipOrderExchange()).with(ORDER_DELAY_KEY);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(vipOrderExchange()).with(ORDER_TIMEOUT_KEY);
    }

    @Bean
    public Binding orderConfirmBinding() {
        return BindingBuilder.bind(orderConfirmQueue()).to(vipOrderExchange()).with(ORDER_CONFIRM_KEY);
    }

    // ===== TCC Confirm 重试队列 Bean =====

    /**
     * TCC Confirm 重试队列：消费失败 NACK 转入死信
     */
    @Bean
    public Queue tccConfirmRetryQueue() {
        return QueueBuilder.durable(TCC_CONFIRM_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", VIP_ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", TCC_CONFIRM_RETRY_DLK)
                .build();
    }

    /**
     * TCC Confirm 重试延迟队列：无消费者，消息 TTL 过期后通过 DLX 回到重试队列实现指数退避
     * DLX 指向业务交换机，routing key 指向重试队列，形成闭环
     */
    @Bean
    public Queue tccConfirmRetryDelayQueue() {
        return QueueBuilder.durable(TCC_CONFIRM_RETRY_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", VIP_ORDER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TCC_CONFIRM_RETRY_KEY)
                .build();
    }

    @Bean
    public Binding tccConfirmRetryBinding() {
        return BindingBuilder.bind(tccConfirmRetryQueue()).to(vipOrderExchange()).with(TCC_CONFIRM_RETRY_KEY);
    }

    @Bean
    public Binding tccConfirmRetryDelayBinding() {
        return BindingBuilder.bind(tccConfirmRetryDelayQueue()).to(vipOrderExchange()).with(TCC_CONFIRM_RETRY_DELAY_KEY);
    }

    // ===== 死信交换机 & 死信队列 Bean & 绑定 =====

    /**
     * VIP 订单死信交换机（DirectExchange，持久化）
     */
    @Bean
    public DirectExchange vipOrderDlx() {
        return new DirectExchange(VIP_ORDER_DLX, true, false);
    }

    @Bean
    public Queue orderTimeoutDlq() {
        return QueueBuilder.durable(ORDER_TIMEOUT_DLQ).build();
    }

    @Bean
    public Queue orderConfirmDlq() {
        return QueueBuilder.durable(ORDER_CONFIRM_DLQ).build();
    }

    /**
     * TCC Confirm 重试死信队列（重试耗尽后人工处理）
     */
    @Bean
    public Queue tccConfirmRetryDlq() {
        return QueueBuilder.durable(TCC_CONFIRM_RETRY_DLQ).build();
    }

    @Bean
    public Binding orderTimeoutDlqBinding() {
        return BindingBuilder.bind(orderTimeoutDlq()).to(vipOrderDlx()).with(ORDER_TIMEOUT_DLK);
    }

    @Bean
    public Binding orderConfirmDlqBinding() {
        return BindingBuilder.bind(orderConfirmDlq()).to(vipOrderDlx()).with(ORDER_CONFIRM_DLK);
    }

    @Bean
    public Binding tccConfirmRetryDlqBinding() {
        return BindingBuilder.bind(tccConfirmRetryDlq()).to(vipOrderDlx()).with(TCC_CONFIRM_RETRY_DLK);
    }
}
