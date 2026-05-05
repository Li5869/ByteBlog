package com.personblog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("智能个人博客系统")
                .version("0.0.1-SNAPSHOT")
                .description("智能个人博客系统接口文档")
                .contact(new Contact().name("person-blog")));
    }
}
