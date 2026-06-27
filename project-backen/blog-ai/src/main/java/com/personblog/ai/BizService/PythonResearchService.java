package com.personblog.ai.BizService;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.personblog.ai.vo.ResearchEventVO;
import com.personblog.common.enums.BizCodeEnum;
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

import static com.personblog.ai.constants.PythonAiApiConstants.*;
import static com.personblog.ai.constants.PythonAiApiConstants.Research.*;

/**
 * Python 深度研究服务调用
 * <p>
 * 负责通过 WebClient 调用 Python AI 服务的研究智能体接口
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonResearchService {

    private final WebClient pythonAiWebClient;

    // ==================== 公开接口 ====================

    /**
     * 启动深度研究任务
     *
     * @param taskId  任务ID（前端生成的UUID）
     * @param message 用户研究需求
     * @return 任务ID
     */
    public Mono<String> startResearch(String taskId, String message) {
        Map<String, String> request = Map.of(
                "task_id", taskId,
                "message", message
        );

        log.info("[Research] 启动研究任务，任务ID: {}", taskId);

        return pythonAiWebClient.post()
                .uri(START)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Integer code = (Integer) response.get(Fields.CODE);
                    if (code != null && code != 0) {
                        String msg = (String) response.get(Fields.MSG);
                        log.error("[Research] Python服务返回业务错误: {}", msg);
                        throw new BizException(BizCodeEnum.AI_PYTHON_BUSINESS_ERROR);
                    }
                    // SSE 流式接口，返回 taskId 即可
                    return taskId;
                })
                .doOnError(e -> log.error("[Research] 启动研究任务失败: {}", e.getMessage()));
    }

    /**
     * 恢复深度研究任务
     *
     * @param taskId   任务ID
     * @param response 用户响应（回答澄清问题 / 确认计划 / 修改意见）
     * @return 任务状态
     */
    public Mono<String> resumeResearch(String taskId, String response) {
        Map<String, String> request = Map.of(
                "task_id", taskId,
                "response", response
        );

        log.info("[Research] 恢复研究任务，任务ID: {}", taskId);

        return pythonAiWebClient.post()
                .uri(RESUME)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    Integer code = (Integer) resp.get(Fields.CODE);
                    if (code != null && code != 0) {
                        String msg = (String) resp.get(Fields.MSG);
                        log.error("[Research] Python服务返回业务错误: {}", msg);
                        throw new BizException(BizCodeEnum.AI_PYTHON_BUSINESS_ERROR);
                    }
                    return taskId;
                })
                .doOnError(e -> log.error("[Research] 恢复研究任务失败: {}", e.getMessage()));
    }

    /**
     * 恢复研究并获取 SSE 事件流
     *
     * @param taskId   任务ID
     * @param response 用户响应
     * @return 研究事件流
     */
    public Flux<ResearchEventVO> streamResumeResearch(String taskId, String response) {
        log.info("[Research] 恢复研究并获取SSE事件流，任务ID: {}", taskId);

        Map<String, String> request = Map.of(
                "task_id", taskId,
                "response", response
        );

        return pythonAiWebClient.post()
                .uri(RESUME)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .filter(event -> event.data() != null)
                .doOnNext(event -> log.info("[Research] 恢复收到SSE事件: data={}", event.data()))
                .map(this::parseSSEEvent)
                .filter(Objects::nonNull)
                .doOnNext(event -> log.info("[Research] 恢复解析后事件: type={}", event.getType()))
                .doOnError(e -> log.error("[Research] 流式恢复研究进度失败: {}", e.getMessage()))
                .onErrorResume(e -> Flux.just(ResearchEventVO.builder()
                        .type(SseEvent.ERROR)
                        .data(Msg.SERVICE_ERROR + e.getMessage())
                        .build()));
    }

    /**
     * 停止深度研究任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    public Mono<Boolean> stopResearch(String taskId) {
        Map<String, String> request = Map.of("task_id", taskId);

        log.info("[Research] 停止研究任务，任务ID: {}", taskId);

        return pythonAiWebClient.post()
                .uri(STOP)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    Integer code = (Integer) resp.get(Fields.CODE);
                    return code != null && code == 0;
                })
                .doOnError(e -> log.error("[Research] 停止研究任务失败: {}", e.getMessage()));
    }

    /**
     * 启动研究并获取 SSE 事件流
     *
     * @param taskId  任务ID
     * @param message 研究需求
     * @return 研究事件流
     */
    public Flux<ResearchEventVO> streamResearch(String taskId, String message) {
        log.info("[Research] 启动研究并获取SSE事件流，任务ID: {}", taskId);

        Map<String, String> request = Map.of(
                "task_id", taskId,
                "message", message
        );

        return pythonAiWebClient.post()
                .uri(START)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .filter(event -> event.data() != null)
                .doOnNext(event -> log.info("[Research] 收到SSE事件: data={}", event.data()))
                .map(this::parseSSEEvent)
                .filter(Objects::nonNull)
                .doOnNext(event -> log.info("[Research] 解析后事件: type={}", event.getType()))
                .doOnError(e -> log.error("[Research] 流式获取研究进度失败: {}", e.getMessage()))
                .onErrorResume(e -> Flux.just(ResearchEventVO.builder()
                        .type(SseEvent.ERROR)
                        .data(Msg.SERVICE_ERROR + e.getMessage())
                        .build()));
    }

    // ==================== 内部方法 ====================

    /**
     * 解析 SSE 事件为 ResearchEventVO
     */
    private ResearchEventVO parseSSEEvent(ServerSentEvent<String> event) {
        try {
            String data = event.data();
            if (data == null || data.isBlank()) {
                return null;
            }

            JSONObject dataMap = JSONUtil.parseObj(data);
            String type = dataMap.getStr(Fields.TYPE);

            log.debug("[Research] 解析SSE事件: type={}", type);

            // done 事件：研究完成
            if ("done".equals(type)) {
                return ResearchEventVO.DONE_EVENT;
            }

            // error 事件：研究异常
            if (SseEvent.ERROR.equals(type)) {
                Object errorData = dataMap.getObj(Fields.DATA);
                log.error("[Research] Python返回错误: {}", errorData);
                return ResearchEventVO.builder()
                        .type(SseEvent.ERROR)
                        .data(errorData)
                        .build();
            }

            // 通用事件：统一按 type + data 构建
            // 使用 get() 而不是 getObj()，保持原始类型（字符串/数组/对象）
            return ResearchEventVO.builder()
                    .type(type)
                    .data(dataMap.get(Fields.DATA))
                    .build();

        } catch (Exception e) {
            log.warn("[Research] 解析SSE事件失败: {} | 原始数据: {}", e.getMessage(), event.data());
            return null;
        }
    }
}
