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
 * TCC 分布式事务记录表
 * 记录每个分支事务的 Try/Confirm/Cancel 状态
 * 支持幂等性、空回滚、悬挂预防、定时补偿
 *
 * @author LSH
 * @since 2026-06-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_tcc_transaction")
public class TccTransaction {

    /** 主键ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 全局事务ID（格式：ORDER_{orderId}） */
    private String xid;

    /** 事务状态：TRYING-尝试中 CONFIRMED-已确认 CANCELLED-已取消 */
    private String status;

    /** 业务类型：POINT_FREEZE-积分冻结 COUPON_FREEZE-优惠券冻结 */
    private String bizType;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数（超过后需人工处理） */
    private Integer maxRetry;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否删除 */
    @TableLogic
    private Boolean isDeleted;
}
