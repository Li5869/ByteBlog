package com.personblog.security.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "个人中心聚合信息")
public class UserProfileStatsVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "性别")
    private Short gender;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "统计信息")
    private Stats stats;

    @Data
    @Schema(description = "统计信息")
    public static class Stats {

        @Schema(description = "原创文章数")
        private Long articleCount;

        @Schema(description = "粉丝数")
        private Long fanCount;

        @Schema(description = "获赞数")
        private Long likeReceivedCount;

        private Long followingCount;

        @Schema(description = "被收藏数")
        private Long collectionCount;

        @Schema(description = "被访问量")
        private Long viewCount;

        @Schema(description = "总评论数")
        private Long commentCount;
    }
}
