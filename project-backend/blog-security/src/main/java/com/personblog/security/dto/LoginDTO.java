package com.personblog.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数 DTO
 * 
 * 用于接收用户登录请求的数据
 * 支持用户名、邮箱、手机号三种方式登录
 * 
 * @author LSH
 */
@Data
@Schema(description = "登录请求参数")
public class LoginDTO {
    
    /**
     * 用户名/邮箱/手机号
     * 
     * 用户可以使用以下任意一种方式登录：
     * - 用户名
     * - 邮箱
     * - 手机号
     */
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名/邮箱/手机号", example = "admin")
    private String username;
    
    /**
     * 密码
     * 
     * 用户密码，明文传输
     * 后端会使用 BCrypt 进行验证
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}
