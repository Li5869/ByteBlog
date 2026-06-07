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

    @Bean
    public Binding orderTimeoutDlqBinding() {
        return BindingBuilder.bind(orderTimeoutDlq()).to(vipOrderDlx()).with(ORDER_TIMEOUT_DLK);
    }

    @Bean
    public Binding orderConfirmDlqBinding() {
        return BindingBuilder.bind(orderConfirmDlq()).to(vipOrderDlx()).with(ORDER_CONFIRM_DLK);
    }
}
