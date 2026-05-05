package com.personblog.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数 DTO
 * 
 * 用于接收用户注册请求的数据
 * 包含必要的验证注解
 * 
 * @author LSH
 */
@Data
@Schema(description = "注册请求参数")
public class RegisterDTO {
    
    /**
     * 用户名
     * 
     * 用户的唯一标识，用于登录
     * 长度限制：3-20 个字符
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在3-20之间")
    @Schema(description = "用户名", example = "testuser")
    private String username;
    
    /**
     * 密码
     * 
     * 用户密码，明文传输
     * 后端会使用 BCrypt 加密后存储
     * 长度限制：6-20 个字符
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在6-20之间")
    @Schema(description = "密码", example = "123456")
    private String password;
    
    /**
     * 邮箱
     * 
     * 可选，用于邮箱登录和找回密码
     */
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "test@example.com")
    private String email;
    
    /**
     * 手机号
     * 
     * 可选，用于手机号登录
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    
    /**
     * 昵称
     * 
     * 可选，用户的显示名称
     * 如果不填，默认使用用户名
     */
    @Schema(description = "昵称", example = "测试用户")
    private String nickname;
}
