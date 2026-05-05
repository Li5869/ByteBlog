package com.personblog.ai.config;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.personblog.ai.constants.LlmPromptType.*;

@Component
public class PromptManger {

    private final Map<String, String> promptMap = new ConcurrentHashMap<>();

    public PromptManger(ResourceLoader resourceLoader) {
        Resource summaryResource = resourceLoader.getResource("classpath:summaryPrompt.txt");
        String summaryPrompt = readPrompt(summaryResource);
        promptMap.put(SUMMARY_TYPE, summaryPrompt);

        Resource titleResource = resourceLoader.getResource("classpath:titlePrompt.txt");
        String titlePrompt = readPrompt(titleResource);
        promptMap.put(TITLE_TYPE, titlePrompt);

        Resource moderationResource = resourceLoader.getResource("classpath:moderationPrompt.txt");
        String moderationPrompt = readPrompt(moderationResource);
        promptMap.put(MODERATION_TYPE, moderationPrompt);

        Resource commentResource = resourceLoader.getResource("classpath:commentPrompt.txt");
        String commentPrompt = readPrompt(commentResource);
        promptMap.put(COMMENT_TYPE, commentPrompt);

        Resource polishResource = resourceLoader.getResource("classpath:polishPrompt.txt");
        String polishPrompt = readPrompt(polishResource);
        promptMap.put(POLISH_TYPE, polishPrompt);
        
        Resource blogDefaultResource = resourceLoader.getResource("classpath:blogDefaultPrompt.txt");
        String blogDefaultPrompt = readPrompt(blogDefaultResource);
        promptMap.put(BLOG_DEFAULT_TYPE, blogDefaultPrompt);

        Resource chatResource = resourceLoader.getResource("classpath:chatPrompt.txt");
        String chatPrompt = readPrompt(chatResource);
        promptMap.put(BLOG_CHAT_TYPE, chatPrompt);

        Resource moderationUserResource = resourceLoader.getResource("classpath:moderationUserPrompt.txt");
        String moderationUserPrompt = readPrompt(moderationUserResource);
        promptMap.put(MODERATION_USER_TYPE, moderationUserPrompt);

        Resource titleUserResource = resourceLoader.getResource("classpath:titleUserPrompt.txt");
        String titleUserPrompt = readPrompt(titleUserResource);
        promptMap.put(TITLE_USER_TYPE, titleUserPrompt);
    }

    public String getPrompt(String type) {
        return promptMap.get(type);
    }

    private String readPrompt(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
