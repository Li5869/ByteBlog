package com.personblog.point.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.personblog.common.constant.PointTypeConstants;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水表
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@TableName("tb_point_log")
public class PointLog {
    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 积分变动值（正数增加，负数减少） */
    private Integer points;

    /** 类型，取值参见 {@link PointTypeConstants} */
    private String type;

    /** 业务ID（可为null，如签到、管理员调整等场景） */
    private Long bizId;

    /** 管理员调整时的描述（type=admin_adjust时使用） */
    private String description;

    /** 操作人ID（管理员调整时使用） */
    private Long operatorId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
