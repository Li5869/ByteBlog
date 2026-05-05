package com.personblog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 管理员操作日志表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_admin_log")
public class AdminLog {

    /** 日志ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作管理员ID(关联tb_user) */
    private Long adminId;

    /** 操作类型: login/logout/create/update/delete/review等 */
    private String actionType;

    /** 操作对象类型: article/user/comment/question等 */
    private String targetType;

    /** 操作对象ID */
    private Long targetId;

    /** 操作描述 */
    private String description;

    /** 操作详情/JSON格式 */
    private String actionDetail;

    /** 操作IP地址 */
    private String ipAddress;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
