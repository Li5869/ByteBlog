package com.personblog.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新个人资料请求")
public class UpdateProfileDTO {

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    @Schema(description = "昵称", example = "李帅豪")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "性别")
    private Short gender;

    @Schema(description = "邮箱")
    private String email;

    @Size(max = 500, message = "简介长度不能超过500个字符")
    @Schema(description = "个人简介", example = "全栈开发者，热爱技术与分享")
    private String bio;
}
