package com.personblog.ai.BizService;

import com.personblog.ai.config.PromptManger;
import com.personblog.ai.dto.ArticlePolishDTO;
import com.personblog.ai.vo.ArticlePolishVO;
import com.personblog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.personblog.ai.constants.LlmPromptType.POLISH_TYPE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticlePolishService {
    private final ChatClient polishChatClient;
    private final PromptManger promptManger;
    public ArticlePolishVO polishArticle(ArticlePolishDTO dto) {
        String prompt = buildPrompt(dto.getContent(), dto.getTitle(), dto.getStyle());

        log.info("开始润色文章，原文长度: {}", dto.getContent().length());

        try {
            String result = polishChatClient.prompt()
                    .system(promptManger.getPrompt(POLISH_TYPE))
                    .user(prompt)
                    .call()
                    .content();

            log.info("文章润色成功，润色后长度: {}", result.length());

            return ArticlePolishVO.builder()
                    .polishedContent(result)
                    .polishNote("文章已润色完成")
                    .build();

        } catch (Exception e) {
            log.error("文章润色失败", e);
            throw new BizException("文章润色失败: " + e.getMessage());
        }
    }

    private String buildPrompt(String content, String title, String style) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请对以下文章进行润色优化，风格要求：");
        
        if ("friendly".equals(style)) {
            prompt.append("友好亲切，通俗易懂。\n\n");
        } else if ("concise".equals(style)) {
            prompt.append("简洁精炼，直击要点。\n\n");
        } else {
            prompt.append("专业严谨，表达准确。\n\n");
        }

        if (StringUtils.hasText(title)) {
            prompt.append("文章标题：").append(title).append("\n\n");
        }

        prompt.append("文章内容：\n").append(content).append("\n\n");
        prompt.append("请直接输出润色后的文章内容，不需要其他解释。");

        return prompt.toString();
    }
}
