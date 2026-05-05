package com.personblog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ消息死信队列错误日志表
 * 记录超过重试次数上限的死信消息，供人工排查和重新投递
 *
 * @author LSH
 */
@Data
@TableName("tb_mq_error_log")
public class MqErrorLog {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 原队列名称 */
    private String queueName;

    /** 原交换机名称 */
    private String exchangeName;

    /** 原路由键 */
    private String routingKey;

    /** 消息体（JSON格式） */
    private String messageBody;

    /** 最后一次错误信息 */
    private String errorMessage;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetryCount;

    /** 状态: PENDING-待处理 RESOLVED-已解决 FAILED-处理失败 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
