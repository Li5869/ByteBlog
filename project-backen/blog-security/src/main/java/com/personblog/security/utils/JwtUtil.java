package com.personblog.security.utils;

import com.personblog.common.api.TokenValidationApi;
import com.personblog.security.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 
 * 提供 JWT Token 的生成、解析、验证功能
 * 使用 JJWT 库实现，支持 HMAC-SHA 签名算法
 * 
 * Token 结构说明：
 * - Header: 算法和令牌类型
 * - Payload: 用户ID（subject）、签发时间、过期时间
 * - Signature: 使用密钥签名，防止篡改
 * 
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil implements TokenValidationApi {
    
    private final JwtProperties jwtProperties;
    
    /**
     * 签名密钥
     * 使用 HMAC-SHA 算法，密钥长度需 >= 256 bits (32 bytes)
     */
    private SecretKey secretKey;
    
    /**
     * 初始化密钥
     * 在 Bean 创建后自动调用，将配置文件中的字符串密钥
     * 转换为 SecretKey 对象
     */
    @PostConstruct
    public void init() {
        // 将字符串密钥转换为 SecretKey 对象
        // Keys.hmacShaKeyFor 会根据密钥长度自动选择合适的 HMAC-SHA 算法
        this.secretKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * 生成 JWT Token
     * Token 中包含的信息：
     * - subject: 用户ID（主要标识）
     * - issuedAt: 签发时间
     * - expiration: 过期时间
     * 
     * @param userId 用户ID
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId) {
        // 当前时间
        Date now = new Date();
        // 过期时间 = 当前时间 + 配置的过期时长
        Date expirationDate = new Date(now.getTime() + jwtProperties.getExpiration());
        
        // 构建 JWT Token
        return Jwts.builder()
                // 设置主题（用户ID），这是 Token 的核心标识
                .subject(String.valueOf(userId))
                // 设置签发时间
                .issuedAt(now)
                // 设置过期时间
                .expiration(expirationDate)
                // 使用密钥签名
                .signWith(secretKey)
                // 压缩生成 Token 字符串
                .compact();
    }
    
    /**
     * 从 Token 中获取用户ID
     * 
     * @param token JWT Token
     * @return 用户ID，Token 无效时返回 null
     */
    @Override
    public Long getUserIdFromToken(String token) {
        try {
            // 解析 Token 并获取 Claims（声明部分）
            Claims claims = Jwts.parser()
                    // 设置验证密钥
                    .verifyWith(secretKey)
                    .build()
                    // 解析并验证签名
                    .parseSignedClaims(token)
                    // 获取 Payload 部分
                    .getPayload();
            
            // subject 中存储的是用户ID的字符串形式
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("解析Token失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证 Token 是否有效
     * 
     * 验证内容：
     * 1. 签名是否正确
     * 2. Token 是否过期
     * 3. Token 格式是否正确
     * 
     * @param token JWT Token
     * @return true 表示有效，false 表示无效
     */
    @Override
    public boolean validateToken(String token) {
        try {
            // 尝试解析 Token，如果成功则说明 Token 有效
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Token 已过期
            log.error("JWT Token已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // 不支持的 JWT 格式
            log.error("不支持的JWT Token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            // Token 格式错误（被篡改或格式不正确）
            log.error("JWT Token格式错误: {}", e.getMessage());
        } catch (SecurityException e) {
            // 签名验证失败（密钥不匹配或 Token 被篡改）
            log.error("JWT签名验证失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // Token 为空或 null
            log.error("JWT Token为空: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 获取 Token 过期时间配置
     * 
     * @return 过期时间（毫秒）
     */
    public long getExpiration() {
        return jwtProperties.getExpiration();
    }

    /**
     * 生成 Refresh Token（长有效期）
     * Refresh Token 使用 "refresh:" 前缀的 subject 来区分 Access Token
     * 防止 Access Token 的验证逻辑错误地接受 Refresh Token
     * 
     * @param userId 用户ID
     * @return Refresh Token 字符串
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                // 使用 "refresh:" 前缀区分 Access Token
                .subject("refresh:" + userId)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Refresh Token 中解析用户ID
     * 验证 subject 必须包含 "refresh:" 前缀
     * 
     * @param token Refresh Token
     * @return 用户ID，Token 无效时返回 null
     */
    public Long getUserIdFromRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 确认是 Refresh Token（含 refresh: 前缀）
            String subject = claims.getSubject();
            if (subject != null && subject.startsWith("refresh:")) {
                return Long.parseLong(subject.substring(8));
            }
            return null;
        } catch (Exception e) {
            log.error("解析Refresh Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Refresh Token 是否有效
     * 验证内容：
     * 1. 签名是否正确
     * 2. Token 是否过期
     * 3. 必须是 Refresh Token（subject 含 refresh: 前缀）
     * 
     * @param token Refresh Token
     * @return true 表示有效，false 表示无效
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 确认是 Refresh Token（含 refresh: 前缀）
            return claims.getSubject() != null && claims.getSubject().startsWith("refresh:");
        } catch (ExpiredJwtException e) {
            log.error("Refresh Token已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT Token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT Token格式错误: {}", e.getMessage());
        } catch (SecurityException e) {
            log.error("JWT签名验证失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT Token为空: {}", e.getMessage());
        }
        return false;
    }
}
