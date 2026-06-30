package com.personblog.common.dto.MqMessage.Vip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VIP 订单确认消息 DTO
 * 用于 MQ 传递订单支付确认信息，异步激活会员
 *
 * @author LSH
 * @since 2026-06-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmMessageDTO {

    /** 订单ID */
    private Long orderId;

    /** 订单编号（便于日志追踪） */
    private String orderNo;

    /** 用户ID */
    private Long userId;
}
