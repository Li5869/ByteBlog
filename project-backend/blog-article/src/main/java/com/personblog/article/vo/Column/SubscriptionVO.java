package com.personblog.article.vo.Column;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅信息返回对象
 *
 * @author LSH
 */
@Data
@Builder
@Schema(description = "订阅信息返回对象")
public class SubscriptionVO {

    @Schema(description = "专栏ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long columnId;

    @Schema(description = "专栏标题")
    private String columnTitle;

    @Schema(description = "专栏封面")
    private String columnCover;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "文章数量")
    private Integer articlesCount;

    @Schema(description = "订阅时间")
    private LocalDateTime subscribedAt;
}
