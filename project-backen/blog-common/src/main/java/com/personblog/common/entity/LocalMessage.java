package com.personblog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 本地消息表（可靠消息投递）
 *
 * @author LSH
 * @since 2026-06-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_local_message")
public class LocalMessage {

    /** 主键ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 业务类型：COUPON_CLAIM-优惠券领取 ORDER_CREATE-订单创建 */
    private String bizType;

    /** 业务ID：优惠券模板ID/订单ID */
    private String bizId;

    /** 业务用户ID：用于幂等去重 */
    private String bizUserId;

    /** RabbitMQ交换机 */
    private String exchange;

    /** RabbitMQ路由键 */
    private String routingKey;

    /** 消息体JSON */
    private String messageBody;

    /** 状态：0-待发送 1-发送中 2-发送成功 3-消费成功 4-发送失败 */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否删除 */
    @TableLogic
    private Boolean isDeleted;
}
