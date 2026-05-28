package com.personblog.ai.BizService;


import cn.hutool.json.JSONUtil;
import com.personblog.ai.config.PromptManger;
import com.personblog.ai.dto.ContentModerationDTO;
import com.personblog.ai.dto.sse.ModerationNotificationDTO;
import com.personblog.ai.vo.ContentModerationVO;
import com.personblog.api.articleAPI.ArticleAPI;
import com.personblog.api.interactionAPI.CommentApi;
import com.personblog.api.interactionAPI.SystemNotificationApi;
import com.personblog.common.dto.MqMessage.notifaction.NotificationMessage;
import com.personblog.push.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.personblog.ai.constants.AiPromptConstants.Moderation;
import static com.personblog.ai.constants.LlmPromptType.MODERATION_TYPE;
import static com.personblog.ai.constants.LlmPromptType.MODERATION_USER_TYPE;
import static com.personblog.common.constant.StatusConstant.APPROVED;
import static com.personblog.common.constant.StatusConstant.REJECT;
import static com.personblog.common.constant.TargetTypeConstant.COMMENT;
import static com.personblog.common.constant.TargetTypeConstant.QUESTION;

/**
 * 内容审核服务实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService{
    private final ChatClient moderationChatClient;
    private final PromptManger promptManger;
    private final ArticleAPI articleAPI;
    private final CommentApi commentApi;
    private final SystemNotificationApi systemNotificationApi;
    private final SseEmitterManager sseEmitterManager;

    /**
     * 执行内容审核
     *
     * @param dto 审核请求 DTO
     * @return 审核状态：approved（通过）/ rejected（拒绝）/ null（审核异常）
     */
    public String moderate(ContentModerationDTO dto) {

        String prompt = buildPrompt(dto);

        log.info("开始内容审核，内容类型: {}", dto.getContentType());

        try {
            String result = moderationChatClient.prompt()
                    .system(promptManger.getPrompt(MODERATION_TYPE))
                    .user(prompt)
                    .call()
                    .content();

            // 解析 JSON 结果
            ContentModerationVO vo = parseResult(result);
            String reviewStatus = vo.getIsViolation() ? REJECT : APPROVED;
            handleReviewStatue(dto.getContentType(), reviewStatus, dto.getBizId());
            log.info("内容审核完成，结果: {}", vo);

            // 发送审核结果通知
            sendModerationNotification(dto, vo, reviewStatus);

            return reviewStatus;

        } catch (Exception e) {
            log.error("内容审核失败", e);
            // 审核失败时，不作处理，留给人工审核
            return null;
        }
    }

    private void handleReviewStatue(String type, String statue, Long bizId) {
        if (type.equals(COMMENT)) {
            commentApi.updateReviewStatue(bizId, statue);
        } else {
            articleAPI.updateArticleReviewStatus(bizId, statue);
        }
    }

    /**
     * 发送审核结果通知
     * 1. SSE 实时推送通知
     * 2. 写入系统通知表
     */
    private void sendModerationNotification(ContentModerationDTO dto, ContentModerationVO vo, String reviewStatus) {
        // 如果没有作者ID，无法发送通知
        if (dto.getAuthorId() == null) {
            log.warn("审核通知发送失败：作者ID为空, bizId={}", dto.getBizId());
            return;
        }

        String title = dto.getTitle() != null ? dto.getTitle() : dto.getContent();

        // 1. SSE 实时推送
        try {
            ModerationNotificationDTO sseMessage = ModerationNotificationDTO.builder()
                    .type(Moderation.SSE_TYPE)
                    .bizType(dto.getContentType())
                    .bizId(dto.getBizId())
                    .reviewStatus(reviewStatus)
                    .title(title)
                    .reason(vo.getReason())
                    .createdAt(LocalDateTime.now())
                    .build();
            sseEmitterManager.sendToUser(dto.getAuthorId(), sseMessage);
            log.info("SSE审核通知推送成功, authorId={}, reviewStatus={}", dto.getAuthorId(), reviewStatus);
        } catch (Exception e) {
            log.error("SSE审核通知推送失败, authorId={}", dto.getAuthorId(), e);
        }

        // 2. 写入系统通知表
        try {
            String actionType = APPROVED.equals(reviewStatus) ? Moderation.ACTION_APPROVED : Moderation.ACTION_REJECTED;

            NotificationMessage notificationDTO = NotificationMessage.builder()
                    .userId(dto.getAuthorId())
                    .actionType(actionType)
                    .targetType(dto.getContentType())
                    .targetId(dto.getBizId())
                    .targetTitle(title)
                    .content(vo.getReason())
                    .createdAt(LocalDateTime.now())
                    .build();

            systemNotificationApi.saveNotification(notificationDTO);
            log.info("系统通知保存成功, authorId={}, actionType={}", dto.getAuthorId(), actionType);
        } catch (Exception e) {
            log.error("系统通知保存失败, authorId={}", dto.getAuthorId(), e);
        }
    }

    private String buildPrompt(ContentModerationDTO dto) {
        String template = promptManger.getPrompt(MODERATION_USER_TYPE);
        String contentTypeDesc = getContentTypeDesc(dto.getContentType());
        String title = dto.getTitle() != null ? dto.getTitle() : dto.getContent();
        return String.format(template, contentTypeDesc, title, dto.getContent());
    }

    private String getContentTypeDesc(String contentType) {
        return switch (contentType) {
            case COMMENT -> Moderation.TYPE_COMMENT;
            case QUESTION -> Moderation.TYPE_QUESTION;
            default -> Moderation.TYPE_ARTICLE;
        };
    }

    private ContentModerationVO parseResult(String result) {
        // 提取 JSON 部分
        String json = extractJson(result);
        return JSONUtil.toBean(json, ContentModerationVO.class);
    }

    private String extractJson(String result) {
        int start = result.indexOf('{');
        int end = result.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return result.substring(start, end + 1);
        }
        return result;
    }
}
