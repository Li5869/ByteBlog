package com.personblog.ai.BizService;

import com.personblog.ai.config.PromptManger;
import com.personblog.ai.dto.ArticleSummaryDTO;
import com.personblog.ai.vo.ArticleSummaryVO;
import com.personblog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

            log.info("文章摘要生成成功，摘要长度: {}", summary.length());

            return ArticleSummaryVO.builder()
                    .summary(summary)
                    .build();

        } catch (Exception e) {
            log.error("文章摘要生成失败", e);
            throw new BizException("文章摘要生成失败: " + e.getMessage());
        }
    }
    private String buildPrompt(String content, String title, Integer maxLength) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下文章生成一段简洁的摘要，控制在")
                .append(maxLength != null ? maxLength : 100)
                .append("字以内。\n\n");

        if (StringUtils.hasText(title)) {
            prompt.append("文章标题：").append(title).append("\n\n");
        }

        prompt.append("文章内容：\n").append(content).append("\n\n");
        prompt.append("请直接输出摘要内容，不需要其他解释。");

        return prompt.toString();
    }
}
