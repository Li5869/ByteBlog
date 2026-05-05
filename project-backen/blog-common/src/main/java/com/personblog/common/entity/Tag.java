package com.personblog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 标签表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_tag")
public class Tag {

    /** 标签ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标签名称 */
    private String name;

    /** 使用次数 */
    private Long useCount;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
