package com.personblog.point.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分排行榜月度记录表
 *
 * @author LSH
 * @since 2026-06-01
 */
@Data
@TableName("tb_point_rank_log")
public class PointRankLog {
    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 年月（格式: 2026-06） */
    private String yearMonth;

    /** 用户ID */
    private Long userId;

    /** 当月积分 */
    private Long points;

    /** 排名 */
    private Integer rankNum;

    /** 用户昵称（冗余，历史快照） */
    private String nickname;

    /** 用户头像（冗余，历史快照） */
    private String avatar;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
