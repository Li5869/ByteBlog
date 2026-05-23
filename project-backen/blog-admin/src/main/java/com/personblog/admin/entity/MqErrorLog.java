package com.personblog.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ消息死信队列错误日志表
 *
 * @author LSH
 */
@Data
@TableName("tb_mq_error_log")
public class MqErrorLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String queueName;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private String errorMessage;

    private Integer retryCount;

    private Integer maxRetryCount;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
