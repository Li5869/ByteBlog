package com.personblog.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签表
 *
 * @author LSH
 */
@Data
@TableName("tb_tag")
public class Tag {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private Long useCount;

    private LocalDateTime createdAt;
}
