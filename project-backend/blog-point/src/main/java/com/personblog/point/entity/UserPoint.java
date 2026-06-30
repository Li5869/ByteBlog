package com.personblog.point.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户积分表
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@TableName("tb_user_point")
public class UserPoint {
    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 累计积分 */
    private Long totalPoints;

    /** 可用积分 */
    private Long availablePoints;

    /** 冻结积分（积分消费预留） */
    private Long frozenPoints;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
