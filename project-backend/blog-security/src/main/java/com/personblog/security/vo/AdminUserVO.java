package com.personblog.security.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 管理端用户列表VO
 *
 * @author LSH
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端用户列表信息")
public class AdminUserVO {

    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "状态：normal/banned")
    private String status;

    @Schema(description = "文章数")
    private Long articleCount;

    @Schema(description = "粉丝数")
    private Long fansCount;

    @Schema(description = "关注数")
    private Long followCount;

    @Schema(description = "获赞数")
    private Long likeCount;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;
}
