package com.personblog.article.vo.Column;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专栏列表返回对象
 *
 * @author LSH
 */
@Data
@Builder
@Schema(description = "专栏列表返回对象")
public class ColumnListVO {

    @Schema(description = "专栏ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "专栏标题")
    private String title;

    @Schema(description = "专栏描述")
    private String description;

    @Schema(description = "专栏封面URL")
    private String cover;

    @Schema(description = "文章数量")
    private Integer articlesCount;

    @Schema(description = "订阅数量")
    private Integer subscriptionCount;

    @Schema(description = "浏览量")
    private Long views;

    @Schema(description = "状态：0-草稿，1-已发布")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "作者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "作者头像")
    private String authorAvatar;
}
