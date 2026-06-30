package com.personblog.ai.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.personblog.ai.constants.LlmPromptType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptManger {

    private final NacosPromptProperties nacosPromptProperties;
    private final Map<String, String> promptMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshPrompts();
        log.info("[PromptManger] 提示词初始化完成，共加载 {} 个提示词", promptMap.size());
    }

    public void refreshPrompts() {
        promptMap.clear();
        
        putIfNotNull(SUMMARY_TYPE, nacosPromptProperties.getSummary(), "摘要生成");
        putIfNotNull(TITLE_TYPE, nacosPromptProperties.getTitle(), "标题生成");
        putIfNotNull(MODERATION_TYPE, nacosPromptProperties.getModeration(), "内容审核");
        putIfNotNull(COMMENT_TYPE, nacosPromptProperties.getComment(), "评论生成");
        putIfNotNull(POLISH_TYPE, nacosPromptProperties.getPolish(), "内容润色");
        putIfNotNull(BLOG_DEFAULT_TYPE, nacosPromptProperties.getBlogDefault(), "博客默认");
        putIfNotNull(BLOG_CHAT_TYPE, nacosPromptProperties.getChat(), "聊天助手");
        putIfNotNull(MODERATION_USER_TYPE, nacosPromptProperties.getModerationUser(), "审核用户提示");
        putIfNotNull(TITLE_USER_TYPE, nacosPromptProperties.getTitleUser(), "标题用户提示");
        
        log.info("[PromptManger] 提示词已刷新，成功加载 {} 个", promptMap.size());
        
        if (promptMap.isEmpty()) {
            log.warn("[PromptManger] 未加载任何提示词，请检查 Nacos 配置是否正确");
        }
    }

    private void putIfNotNull(String key, String value, String description) {
        if (value != null && !value.isBlank()) {
            promptMap.put(key, value);
            log.debug("[PromptManger] 加载提示词: {}", description);
        } else {
            log.warn("[PromptManger] 提示词配置为空: {} ({})", description, key);
        }
    }

    public String getPrompt(String type) {
        String prompt = promptMap.get(type);
        if (prompt == null) {
            log.warn("[PromptManger] 未找到类型为 {} 的提示词，请检查 Nacos 配置", type);
        }
        return prompt;
    }
}
