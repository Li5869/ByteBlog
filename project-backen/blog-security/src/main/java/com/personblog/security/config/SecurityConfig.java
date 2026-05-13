package com.personblog.security.config;

import com.personblog.security.filter.ApiKeyAuthFilter;
import com.personblog.security.filter.JwtAuthenticationTokenFilter;
import com.personblog.security.handler.AccessDeniedHandlerImpl;
import com.personblog.security.handler.AuthenticationEntryPointImpl;
import com.personblog.security.handler.LogoutSuccessHandlerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 核心配置类
 * 
 * 配置内容：
 * 1. 安全过滤器链（SecurityFilterChain）
 * 2. 白名单路径（无需认证即可访问）
 * 3. 权限控制（管理员路径需要 admin 权限）
 * 4. JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前执行）
 * 5. 异常处理（认证失败、授权失败、登出）
 * 6. CORS 跨域配置
 * 7. 密码加密器（BCrypt）
 * 8. 认证管理器（AuthenticationManager）
 * 
 * @author LSH
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final LogoutSuccessHandlerImpl logoutSuccessHandler;
    
    /**
     * 配置安全过滤器链
     * 
     * 这是 Spring Security 的核心配置
     * 定义了请求的安全规则和过滤器链
     * 
     * @param http HttpSecurity 对象
     * @return SecurityFilterChain 安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // 1. 禁用 CSRF（跨站请求伪造保护）
                // 因为使用 JWT 无状态认证，不需要 CSRF 保护
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 配置 Session 管理策略
                // STATELESS: 不创建和使用 Session，完全无状态
                // 这是 JWT 认证的标准配置
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/captcha",
                                "/auth/refresh",
                                "/doc.html",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/article/banners",
                                "/article/articles",
                                "/article/articles/random",
                                "/article/articles/hot",
                                "/article/articles/{id}",
                                "/article/articles/{id}/related",
                                "/article/tags/hot",
                                "/user/active",
                                "/user/profile/**",
                                "/user/users/{id}",
                                "/tag/**",
                                "/category/**",
                                "/error",
                                "/comment/comments/article/{articleId}",
                                "/question/questions/hot",
                                "/question/questions",
                                "/question/questions/{id}/answers",
                                "/question/questions/{id}",
                                "/ws/**",
                                "/interaction/sse/**",
                                "/search/**",
                                "/article/columns/hot",
                                "/article/columns/list",
                                "/article/articles/{id}",
                                "/article/articles/{id}/interaction",
                                "/article/columns/{id}"
                        ).permitAll()

                        // 3.2 管理端登录/刷新接口（无需认证，放在 /admin/** 之前）
                        .requestMatchers(
                                "/admin/auth/login",
                                "/admin/auth/refresh"
                        ).permitAll()

                        // 3.3 Actuator 监控端点（生产环境建议限制 IP 或加认证）
                        .requestMatchers("/actuator/**").permitAll()

                        // 3.4 管理员路径：需要 admin 权限
                        .requestMatchers("/admin/**").hasAuthority("admin")

                        // 3.5 其他所有请求：需要认证
                        .anyRequest().authenticated()
                )

                // 4. 添加 API Key 过滤器和 JWT 过滤器
                // API Key 过滤器先执行（用于内部服务间认证）
                // JWT 过滤器后执行（用于用户认证）
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)

                // 5. 配置异常处理
                .exceptionHandling(exception -> exception
                        // 认证失败处理器（未登录访问受保护资源）
                        .authenticationEntryPoint(authenticationEntryPoint)
                        // 授权失败处理器（无权限访问）
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // 6. 配置登出
                .logout(logout -> logout
                        // 登出 URL
                        .logoutUrl("/auth/logout")
                        // 登出成功处理器
                        .logoutSuccessHandler(logoutSuccessHandler)
                )

                // 7. 配置 CORS 跨域
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }
    
    /**
     * 配置 CORS 跨域
     * 
     * 允许前端跨域访问后端接口
     * 
     * @return CorsConfigurationSource CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许所有来源（生产环境建议配置具体域名）
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许所有请求头
        configuration.setAllowedHeaders(List.of("*"));
        
        // 允许携带凭证（Cookie、Authorization 头等）
        configuration.setAllowCredentials(true);
        
        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);
        
        // 注册 CORS 配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * 密码加密器
     * 
     * 使用 BCrypt 算法加密密码
     * BCrypt 是一种安全的单向哈希算法，自带盐值
     * 
     * @return PasswordEncoder 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 认证管理器
     * 
     * 用于处理认证请求
     * 在登录时会调用 authenticationManager.authenticate()
     * 
     * @param config 认证配置
     * @return AuthenticationManager 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
