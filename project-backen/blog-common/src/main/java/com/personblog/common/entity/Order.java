package com.personblog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用订单表
 * <p>所有业务（VIP会员、付费专栏等）共用，通过 biz_type 区分业务类型</p>
 * @author LSH
 */
@Data
@TableName("tb_order")
public class Order {

    /** 主键ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单号（业务前缀+雪花ID，如VIP1929837465123456789） */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 业务类型：1-VIP会员 2-付费专栏 */
    private Short bizType;

    /** 业务ID（套餐ID / 专栏ID） */
    private Long bizId;

    /** 业务快照JSON（数据库JSONB，Java用String接收） */
    private String bizSnapshot;

    /** 积分原价（套餐积分价格） */
    private Integer pointsCost;

    /** 优惠券ID（可为空） */
    private Long couponId;

    /** 优惠券减免积分数 */
    private Integer couponDiscount;

    /** 实际冻结积分（= pointsCost - couponDiscount） */
    private Integer actualPoints;

    /** 订单状态：0-待确认 1-已冻结 2-已完成 3-已取消 4-已关闭 */
    private Short status;

    /** 订单过期时间（15分钟，超时触发Cancel） */
    private LocalDateTime expireTime;

    /** 取消原因 */
    private String cancelReason;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除 */
    @TableLogic
    private Boolean isDeleted;
}
