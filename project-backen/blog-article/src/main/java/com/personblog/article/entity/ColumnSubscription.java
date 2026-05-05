package com.personblog.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专栏订阅表
 *
 * @author LSH
 */
@Data
@TableName("tb_column_subscription")
public class ColumnSubscription {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订阅用户ID */
    private Long userId;

    /** 订阅的专栏ID */
    private Long columnId;

    /** 订阅时间 */
    private LocalDateTime createdAt;
}
