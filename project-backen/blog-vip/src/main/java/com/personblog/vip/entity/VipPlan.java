package com.personblog.vip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VIP套餐表
 * @author LSH
 */
@Data
@TableName("tb_vip_plan")
public class VipPlan {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 套餐编码 */
    private String planCode;

    /** 套餐名称 */
    private String planName;

    /** 时长（月） */
    private Integer durationMonths;

    /** 积分价格 */
    private Integer pointsPrice;

    /** 排序权重 */
    private Integer sortOrder;

    /** 状态：0-下架 1-上架 */
    private Short status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
