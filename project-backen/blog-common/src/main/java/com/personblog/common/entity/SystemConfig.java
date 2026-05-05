package com.personblog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 系统配置表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_system_config")
public class SystemConfig {

    /** 配置ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置类型: boolean/string/number/json */
    private String configType;

    /** 配置描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
