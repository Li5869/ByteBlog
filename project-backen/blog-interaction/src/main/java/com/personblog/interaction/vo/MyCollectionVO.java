package com.personblog.interaction.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "我的收藏VO")
public class MyCollectionVO {

    @Schema(description = "收藏ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "文章ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long articleId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章封面")
    private String cover;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "作者头像")
    private String authorAvatar;

    @Schema(description = "文章创作时间")
    private LocalDateTime createdAt;

    @Schema(description = "收藏时间")
    private LocalDateTime collectedAt;

    @Schema(description = "文章是否已删除")
    private Boolean isDeleted;
}
