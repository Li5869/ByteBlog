package com.personblog.vip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员状态表
 * @author LSH
 */
@Data
@TableName("tb_vip_membership")
public class VipMembership {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 会员等级：0-普通 1-VIP */
    private Short vipLevel;

    /** 会员期开始时间 */
    private LocalDateTime startTime;

    /** 会员期到期时间 */
    private LocalDateTime endTime;

    /** 累计开通月数 */
    private Integer totalMonths;

    /** 最近一笔订单ID */
    private Long lastOrderId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
