package com.personblog.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员操作日志表
 *
 * @author LSH
 */
@Data
@TableName("tb_admin_log")
public class AdminLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long adminId;

    private String actionType;

    private String targetType;

    private Long targetId;

    private String description;

    private String actionDetail;

    private String ipAddress;

    private LocalDateTime createdAt;
}
