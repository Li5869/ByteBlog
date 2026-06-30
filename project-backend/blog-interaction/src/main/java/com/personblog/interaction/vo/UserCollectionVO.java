package com.personblog.interaction.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "用户收藏VO")
public class UserCollectionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "收藏ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章封面")
    private String cover;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "作者头像")
    private String authorAvatar;

    @Schema(description = "收藏时间")
    private LocalDateTime collectedAt;

    @Schema(description = "文章是否已删除")
    private Boolean isDeleted;
}
