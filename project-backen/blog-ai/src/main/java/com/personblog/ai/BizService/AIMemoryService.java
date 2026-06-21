package com.personblog.ai.BizService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.personblog.ai.dto.ConversationActiveDTO;
import com.personblog.common.result.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.personblog.ai.constants.PythonAiApiConstants.Memory.EXTRACT;
import static com.personblog.ai.constants.RedisKeys.REDIS_CHAT_ACTIVE_PREFIX;

/**
 * AI 记忆提取服务
 * 扫描 Redis 中过期的对话活跃标记，调用 Python 端批量提取用户长期记忆
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final WebClient pythonAiWebClient;

    /** 对话过期阈值（分钟）：最后活跃时间超过此值视为对话结束 */
    private static final long STALE_THRESHOLD_MINUTES = 3;

    /**
     * 扫描过期对话并提取记忆
     * 由 XXL-Job 定时触发或通过 MemoryExtractApi 手动调用
     */
    public void extractMemories() {
        // 1. 扫描 Redis 中过期的对话活跃标记
        List<ConversationActiveDTO> staleConversations = scanStaleConversations();
        if (CollUtil.isEmpty(staleConversations)) {
            log.debug("[记忆提取] 无过期对话，跳过处理");
            return;
        }

        log.info("[记忆提取] 发现 {} 个过期对话，开始调用 Python 端提取", staleConversations.size());

        // 2. 调用 Python 端记忆提取 API（异步，回调中清理 Redis Key）
        callPythonExtractApi(staleConversations);
    }

    /**
     * 扫描 Redis 中 chat:active:* 的 Key，过滤最后活跃时间超过阈值的过期对话
     */
    private List<ConversationActiveDTO> scanStaleConversations() {
        Set<String> keys = redisTemplate.keys(REDIS_CHAT_ACTIVE_PREFIX + "*");
        if (CollUtil.isEmpty(keys)) {
            return List.of();
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES);
        List<ConversationActiveDTO> staleList = new ArrayList<>();

        for (String key : keys) {
            String value = redisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(value)) {
                continue;
            }

            try {
                JSONObject data = JSONUtil.parseObj(value);
                // Redis Value 中的 last_active 是 ISO 时间字符串（Python datetime.isoformat()）
                String lastActiveStr = data.getStr("last_active");
                if (StrUtil.isBlank(lastActiveStr)) {
                    log.warn("[记忆提取] Key {} 中缺少 last_active 字段，跳过", key);
                    continue;
                }

                LocalDateTime lastActive = LocalDateTimeUtil.parse(lastActiveStr);
                // 最后活跃时间早于阈值 → 对话已结束
                if (lastActive.isBefore(threshold)) {
                    String conversationId = key.substring(REDIS_CHAT_ACTIVE_PREFIX.length());
                    String userId = data.getStr("user_id");
                    staleList.add(ConversationActiveDTO.builder()
                            .conversationId(conversationId)
                            .userId(userId)
                            .build());
                }
            } catch (Exception e) {
                log.warn("[记忆提取] 解析 Key {} 失败: {}", key, e.getMessage());
            }
        }
        return staleList;
    }

    /**
     * 通过 WebClient 调用 Python 端记忆提取 API
     * 异步发送，在成功回调中清理 Redis 活跃标记
     */
    private void callPythonExtractApi(List<ConversationActiveDTO> conversations) {
        // 构建请求体：{"conversations": [{"conversation_id": "...", "user_id": "..."}]}
        List<Map<String, String>> convList = conversations.stream()
                .map(c -> Map.of("conversation_id", c.getConversationId(), "user_id", c.getUserId()))
                .toList();
        Map<String, Object> body = Map.of("conversations", convList);

        pythonAiWebClient.post()
                .uri(EXTRACT)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonData.class)
                .subscribe(
                        response -> {
                            // 成功：记录日志并清理 Redis 活跃标记
                            log.info("[记忆提取] Python 端处理成功，响应: {}", JSONUtil.toJsonStr(response));
                            cleanupRedisKeys(conversations);
                        },
                        error -> {
                            // 失败：记录错误日志，由 XXL-Job 重试机制兜底
                            log.error("[记忆提取] 调用 Python 端失败，待下次扫描重试: {}", error.getMessage());
                        }
                );
    }

    /**
     * 删除已处理的 Redis 活跃标记，防止重复提取
     */
    private void cleanupRedisKeys(List<ConversationActiveDTO> conversations) {
        List<String> keysToDelete = conversations.stream()
                .map(c -> REDIS_CHAT_ACTIVE_PREFIX + c.getConversationId())
                .toList();
        redisTemplate.delete(keysToDelete);
        log.info("[记忆提取] 已清理 {} 个 Redis 活跃标记", keysToDelete.size());
    }
}
