package com.personblog.ai.BizService;

import com.personblog.ai.config.PromptManger;
import com.personblog.ai.dto.ArticleSummaryDTO;
import com.personblog.ai.vo.ArticleSummaryVO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.personblog.ai.constants.AiPromptConstants.Summary;
import static com.personblog.ai.constants.LlmPromptType.SUMMARY_TYPE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleSummaryService {
    private final ChatClient summaryChatClient;

    private final PromptManger promptManger;
    public ArticleSummaryVO generateSummary(ArticleSummaryDTO dto) {
        // 构建提示词
        String prompt = buildPrompt(dto.getContent(), dto.getTitle(), dto.getMaxLength());

        log.info("开始生成文章摘要，原文长度: {}", dto.getContent().length());

        try {
            // 调用 AI 生成摘要
            String summary = summaryChatClient.prompt()
                    .system(promptManger.getPrompt(SUMMARY_TYPE))
                    .user(prompt)
                    .call()
                    .content();

            if (summary != null) {
                log.info("文章摘要生成成功，摘要长度: {}", summary.length());
            }

            return ArticleSummaryVO.builder()
                    .summary(summary)
                    .build();

        } catch (Exception e) {
            log.error("文章摘要生成失败", e);
            throw new BizException(BizCodeEnum.AI_SUMMARY_ERROR);
        }
    }
    private String buildPrompt(String content, String title, Integer maxLength) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(Summary.PREFIX)
                .append(maxLength != null ? maxLength : Summary.DEFAULT_MAX_LENGTH)
                .append(Summary.SUFFIX);

        if (StringUtils.hasText(title)) {
            prompt.append(Summary.TITLE_LABEL).append(title).append("\n\n");
        }

        prompt.append(Summary.CONTENT_LABEL).append(content).append("\n\n");
        prompt.append(Summary.OUTPUT_INSTRUCTION);

        return prompt.toString();
    }
}
