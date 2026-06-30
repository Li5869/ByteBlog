package com.personblog.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "ai.prompts")
public class NacosPromptProperties {

    private String blogDefault;
    private String chat;
    private String comment;
    private String moderation;
    private String moderationUser;
    private String polish;
    private String summary;
    private String title;
    private String titleUser;
}
