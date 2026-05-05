package com.personblog.security.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "作者信息返回对象")
public class AuthorInfoVO {

    @JsonSerialize(using = ToStringSerializer.class)
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

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "是否为管理员")
    private Boolean isAdmin;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;

    @Schema(description = "文章数量")
    private Long articleCount;

    @Schema(description = "关注数量")
    private Long followCount;

    @Schema(description = "粉丝数量")
    private Long fansCount;

    @Schema(description = "获赞数量")
    private Long likeCount;

    @Schema(description = "收藏数量")
    private Long collectionCount;

}
