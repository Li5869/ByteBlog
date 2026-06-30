package com.personblog.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专栏表
 *
 * @author LSH
 */
@Data
@TableName("tb_column")
public class Column {

    /** 专栏ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID(关联tb_user) */
    private Long userId;

    /** 专栏标题 */
    private String title;

    /** 专栏描述 */
    private String description;

    /** 专栏封面URL */
    private String cover;

    /** 文章数量 */
    private Integer articlesCount;

    /** 订阅数量 */
    private Integer subscriptionCount;

    /** 浏览量 */
    private Long views;

    /** 状态: 0-草稿, 1-已发布 */
    private Integer status;

    /** 逻辑删除标记 */
    @TableLogic
    private Boolean isDeleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
