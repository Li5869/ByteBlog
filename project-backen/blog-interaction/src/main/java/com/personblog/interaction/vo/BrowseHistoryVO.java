package com.personblog.interaction.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "浏览历史VO")
public class BrowseHistoryVO {

    @Schema(description = "文章ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long articleId;

    @Schema(description = "文章标题")
    private String articleTitle;

    @Schema(description = "文章封面")
    private String articleCover;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "作者头像")
    private String authorAvatar;

    @Schema(description = "文章创作时间")
    private LocalDateTime createdAt;

    @Schema(description = "浏览时间")
    private LocalDateTime browseTime;

    @Schema(description = "文章是否已删除")
    private Boolean isDeleted;
}
