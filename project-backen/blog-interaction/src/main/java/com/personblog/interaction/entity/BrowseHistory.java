package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 浏览历史表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_browse_history")
public class BrowseHistory {

    /** 历史记录ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID(关联tb_user) */
    private Long userId;

    /** 文章ID(关联tb_article) */
    private Long articleId;

    /** 浏览更新时间(用于排序) */
    private LocalDateTime browseAt;
}
