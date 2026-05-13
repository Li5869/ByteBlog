package com.personblog.ai.BizService;

import com.personblog.ai.config.PromptManger;
import com.personblog.ai.dto.ArticlePolishDTO;
import com.personblog.ai.vo.ArticlePolishVO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.personblog.ai.constants.AiBusinessConstants.PolishStyle;
import static com.personblog.ai.constants.AiPromptConstants.Polish;
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

            if (result != null) {
                log.info("文章润色成功，润色后长度: {}", result.length());
            }

            return ArticlePolishVO.builder()
                    .polishedContent(result)
                    .polishNote(Polish.POLISH_NOTE)
                    .build();

        } catch (Exception e) {
            log.error("文章润色失败", e);
            throw new BizException(BizCodeEnum.AI_POLISH_ERROR);
        }
    }

    private String buildPrompt(String content, String title, String style) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append(Polish.PREFIX);
        
        if (PolishStyle.FRIENDLY.equals(style)) {
            prompt.append(Polish.STYLE_FRIENDLY);
        } else if (PolishStyle.CONCISE.equals(style)) {
            prompt.append(Polish.STYLE_CONCISE);
        } else {
            prompt.append(Polish.STYLE_PROFESSIONAL);
        }

        if (StringUtils.hasText(title)) {
            prompt.append(Polish.TITLE_LABEL).append(title).append("\n\n");
        }

        prompt.append(Polish.CONTENT_LABEL).append(content).append("\n\n");
        prompt.append(Polish.OUTPUT_INSTRUCTION);

        return prompt.toString();
    }
}
