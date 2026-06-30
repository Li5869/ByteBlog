package com.personblog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@MapperScan("com.personblog.**.mapper")
@ConfigurationPropertiesScan
@EnableAsync
@EnableCaching
public class PersonBlogApplication {
    public static void main(String[] args) {
        loadEnvFile(Path.of("project-backend/.env"));
        SpringApplication.run(PersonBlogApplication.class, args);
    }
    /**
     * 加载 .env 文件中的环境变量到系统属性中
     * 仅当环境变量尚未设置时才写入，允许已存在的环境变量优先
     */
    private static void loadEnvFile(Path envPath) {
        try {
            if (Files.notExists(envPath)) {
                System.err.println("[WARN] .env file not found: " + envPath.toAbsolutePath());
                return;
            }
            Files.lines(envPath)
                    .map(String::trim)
                    .filter(line -> !line.startsWith("#") && line.contains("="))
                    .forEach(line -> {
                        int eqIndex = line.indexOf('=');
                        String key = line.substring(0, eqIndex).trim();
                        String value = line.substring(eqIndex + 1).trim();
                        // 只在环境变量未设置时写入，避免覆盖已存在的环境变量
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    });
            System.out.println("[INFO] Successfully loaded .env file: " + envPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[WARN] Failed to load .env file: " + e.getMessage());
        }
    }

}