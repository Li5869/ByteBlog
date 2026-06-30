package com.personblog.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性类
 * 
 * 用于从 application.yml 中读取 jwt 相关配置
 * 配置示例:
 * jwt:
 *   secret: JWT_SECRET_KEY=xxx
 *   expiration: 1800000     # Access Token: 30分钟（毫秒）
 *   refresh-token-expiration: 604800000  # Refresh Token: 7天（毫秒）
 * 
 * 请求头格式：token: <token>
 * 
 * @author LSH
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * JWT 签名密钥
     * 生产环境务必使用强密钥，建议 256 位以上
     */
    private String secret;
    
    /**
     * Access Token 过期时间（毫秒）
     * 默认 30 分钟 = 1800000 毫秒
     */
    private Long expiration;
    
    /**
     * Refresh Token 过期时间（毫秒）
     * 用于签发 Refresh Token 的有效期
     * 默认 7 天 = 604800000 毫秒
     */
    private Long refreshTokenExpiration;
}
