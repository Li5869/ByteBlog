package com.personblog.point.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 签到记录表
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@TableName("tb_sign_record")
public class SignRecord {
    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 签到日期 */
    private LocalDate signDate;

    /** 获得积分 */
    private Integer points;

    /** 连续签到天数 */
    private Integer continuousDays;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
