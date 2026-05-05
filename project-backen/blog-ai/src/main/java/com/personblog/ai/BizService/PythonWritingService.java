package com.personblog.ai.BizService;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.personblog.ai.dto.PythonWritingRequest;
import com.personblog.ai.vo.WritingEventVO;
import com.personblog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Python写作服务调用
 * <p>
 * 负责通过 WebClient 调用 Python AI 服务的写作智能体接口
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonWritingService {

    private final WebClient pythonAiWebClient;

    // ==================== API 路径常量 ====================
    private static final String WRITING_START = "/api/v1/writing/start";
    private static final String WRITING_RESUME = "/api/v1/writing/{taskId}/resume";
    private static final String WRITING_STREAM = "/api/v1/writing/{taskId}/stream";
    private static final String WRITING_STATUS = "/api/v1/writing/{taskId}/status";

    // ==================== JSON 字段常量 ====================
    private static final String FIELD_CODE = "code";
    private static final String FIELD_MSG = "msg";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TASK_ID = "task_id";
    private static final String FIELD_STATUS = "status";

    /**
     * SSE 事件中，除 done 和 error 外的通用事件类型集合
     * 这些事件统一使用 "type + data" 结构构建 VO
     */
    private static final Set<String> GENERIC_EVENT_TYPES = Set.of(
            "phase", "plan_ready", "token", "reflection_result", "finalize_ready"
    );

    // ==================== 公开接口 ====================

    /**
     * 启动写作任务
     *
     * @param taskId  任务ID
     * @param message 用户写作需求
     * @return 任务ID
     */
    public Mono<String> startWriting(String taskId, String message) {
        PythonWritingRequest request = PythonWritingRequest.builder()
                .taskId(taskId)
                .message(message)
                .build();

        log.info("[Writing] 启动写作任务，任务ID: {}", taskId);

        return pythonAiWebClient.post()
                .uri(WRITING_START)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> extractDataField(response, FIELD_TASK_ID))
                .doOnError(e -> log.error("[Writing] 启动写作任务失败: {}", e.getMessage()));
    }

    /**
     * 恢复写作任务
     * <p>
     * 用户审核大纲后，批准执行或要求修改
     *
     * @param taskId   任务ID
     * @param action   操作类型：approve-批准执行, revise-修改大纲
     * @param feedback 修改意见（仅当action=revise时需要）
     * @return 任务状态
     */
    public Mono<String> resumeWriting(String taskId, String action, String feedback) {
        PythonWritingRequest request = PythonWritingRequest.builder()
                .action(action)
                .feedback(feedback)
                .build();

        log.info("[Writing] 恢复写作任务，任务ID: {}, 操作: {}", taskId, action);

        return pythonAiWebClient.post()
                .uri(WRITING_RESUME, taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> extractDataField(response, FIELD_STATUS))
                .doOnError(e -> log.error("[Writing] 恢复写作任务失败: {}", e.getMessage()));
    }

    /**
     * 流式获取写作进度
     * <p>
     * 通过 SSE 实时返回写作各阶段的进度事件
     *
     * @param taskId 任务ID
     * @return 写作事件流
     */
    public Flux<WritingEventVO> streamWriting(String taskId) {
        log.info("[Writing] 开始流式获取写作进度，任务ID: {}", taskId);

        return pythonAiWebClient.get()
                .uri(WRITING_STREAM, taskId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .filter(event -> event.data() != null)
                .doOnNext(event -> log.info("[Writing] 收到SSE事件: data={}", event.data()))
                .map(this::parseSSEEvent)
                .filter(Objects::nonNull)
                .doOnNext(event -> log.info("[Writing] 解析后事件: type={}", event.getType()))
                .doOnError(e -> log.error("[Writing] 流式获取写作进度失败: {}", e.getMessage()))
                .onErrorResume(e -> Flux.just(WritingEventVO.builder()
                        .type("error")
                        .data("服务异常: " + e.getMessage())
                        .build()));
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    public Mono<String> getTaskStatus(String taskId) {
        return pythonAiWebClient.get()
                .uri(WRITING_STATUS, taskId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> extractDataField(response, FIELD_STATUS))
                .doOnError(e -> log.error("[Writing] 获取任务状态失败: {}", e.getMessage()));
    }

    // ==================== 内部方法 ====================

    /**
     * 统一解析 Python 服务响应，提取 data 中的指定字段
     * <p>
     * Python 服务统一响应格式：{"code": 0, "msg": "success", "data": {...}}
     *
     * @param response  原始响应 Map
     * @param fieldName 要从 data 中提取的字段名
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    private String extractDataField(Map<String, Object> response, String fieldName) {
        Integer code = (Integer) response.get(FIELD_CODE);
        if (code != null && code != 0) {
            String msg = (String) response.get(FIELD_MSG);
            throw new BizException(msg != null ? msg : "Python服务返回错误");
        }

        Map<String, Object> data = (Map<String, Object>) response.get(FIELD_DATA);
        if (data == null) {
            throw new BizException("Python服务返回数据为空");
        }

        return (String) data.get(fieldName);
    }

    /**
     * 解析 SSE 事件为 WritingEventVO
     * <p>
     * 通用事件类型（phase/plan_ready/token 等）统一按 type + data 构建；
     * done 和 error 事件单独处理
     *
     * @param event 原始 SSE 事件
     * @return 解析后的事件 VO，解析失败返回 null
     */
    private WritingEventVO parseSSEEvent(ServerSentEvent<String> event) {
        try {
            String data = event.data();
            if (data == null || data.isBlank()) {
                return null;
            }

            JSONObject dataMap = JSONUtil.parseObj(data);
            String type = dataMap.getStr(FIELD_TYPE);

            log.debug("[Writing] 解析SSE事件: type={}", type);

            // done 事件：写作完成
            if ("done".equals(type)) {
                return WritingEventVO.DONE_EVENT;
            }

            // error 事件：写作异常
            if ("error".equals(type)) {
                Object errorData = dataMap.getObj(FIELD_DATA);
                log.error("[Writing] Python返回错误: {}", errorData);
                return WritingEventVO.builder()
                        .type("error")
                        .data(errorData)
                        .build();
            }

            // 通用事件：统一按 type + data 构建
            return WritingEventVO.builder()
                    .type(type)
                    .data(GENERIC_EVENT_TYPES.contains(type) ? dataMap.getObj(FIELD_DATA) : dataMap)
                    .build();

        } catch (Exception e) {
            log.warn("[Writing] 解析SSE事件失败: {} | 原始数据: {}", e.getMessage(), event.data());
            return null;
        }
    }
}
