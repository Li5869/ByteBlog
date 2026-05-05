package com.personblog.ai.BizService;


import com.personblog.ai.config.PromptManger;
import com.personblog.ai.dto.ArticleTitleDTO;
import com.personblog.ai.vo.ArticleTitleVO;
import com.personblog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static com.personblog.ai.constants.LlmPromptType.TITLE_TYPE;
import static com.personblog.ai.constants.LlmPromptType.TITLE_USER_TYPE;

/**
 * 文章标题服务实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleTitleService{
    private final ChatClient titleChatClient;
    private final PromptManger promptManger;
    public ArticleTitleVO generateTitle(ArticleTitleDTO dto) {

        String content = truncateContent(dto.getContent());
        String prompt = buildPrompt(content, dto.getMaxLength(), dto.getStyle());

        log.info("开始生成文章标题");

        try {
            String result = titleChatClient.prompt()
                    .system(promptManger.getPrompt(TITLE_TYPE))
                    .user(prompt)
                    .call()
                    .content();

            List<String> titles = parseTitles(result);

            log.info("文章标题生成成功，生成 {} 个候选标题", titles.size());

            return ArticleTitleVO.builder()
                    .titles(titles)
                    .build();

        } catch (Exception e) {
            log.error("文章标题生成失败", e);
            throw new BizException("文章标题生成失败: " + e.getMessage());
        }
    }

    private String buildPrompt(String content, Integer maxLength, String style) {
        String template = promptManger.getPrompt(TITLE_USER_TYPE);
        return String.format(template, style, maxLength != null ? maxLength : 30, content);
    }

    private String truncateContent(String content) {
        if (content.length() <= 3000) {
            return content;
        }
        return content.substring(0, 3000) + "...";
    }

    private List<String> parseTitles(String result) {
        if (result == null || result.isBlank()) {
            return List.of();
        }

        return Arrays.stream(result.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.replaceAll("^[0-9]+[.、)）]\\s*", ""))
                .toList();
    }
}