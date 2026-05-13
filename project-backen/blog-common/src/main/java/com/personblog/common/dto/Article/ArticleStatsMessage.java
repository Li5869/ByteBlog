package com.personblog.common.dto.Article;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 文章统计更新 MQ 消息
 * <p>
 * 发送到 MQ 异步处理文章创建/删除后的统计更新操作，
 * 包括更新用户文章数、标签使用次数、分类文章数、清理标签缓存等。
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文章ID */
    private Long articleId;

    /** 标签ID集合（用于更新标签使用次数） */
    private Set<Long> tagIds;

    /** 分类ID（用于更新分类文章数） */
    private Long categoryId;

    /** 作者用户ID（用于更新用户文章数） */
    private Long userId;

    /** 增减量：1（创建/发布）或 -1（删除/下架） */
    private Integer delta;
}