package com.personblog.ai.controller.CallPythonController;

import com.personblog.ai.BizService.PythonResearchService;
import com.personblog.ai.BizService.ResearchBizService;
import com.personblog.ai.dto.ResearchResumeDTO;
import com.personblog.ai.dto.ResearchStartDTO;
import com.personblog.ai.entity.ResearchTask;
import com.personblog.ai.vo.ResearchEventVO;
import com.personblog.ai.vo.ResearchHistoryVO;
import com.personblog.ai.vo.ResearchReportVO;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static reactor.core.scheduler.Schedulers.boundedElastic;

/**
 * 深度研究智能体控制器
 *
 * @author LSH
 */
@Tag(name = "深度研究智能体", description = "深度研究相关接口，支持流式返回研究进度")
@Slf4j
@RestController
@RequestMapping("/ai/research")
@RequiredArgsConstructor
public class ResearchAgentController {

    private final PythonResearchService pythonResearchService;
    private final ResearchBizService researchBizService;

    @Operation(summary = "创建并启动研究任务", description = "在数据库中创建研究任务记录，并调用AI服务开始深度研究，返回SSE事件流")
    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResearchEventVO> startResearch(@Valid @RequestBody ResearchStartDTO dto) {
        Long userId = UserContextHolder.getUserId();
        String taskId = dto.getTaskId();

        // 创建任务记录
        return Mono.fromCallable(() -> researchBizService.createTask(taskId, userId, dto.getMessage()))
                .subscribeOn(boundedElastic())
                .thenMany(pythonResearchService.streamResearch(taskId, dto.getMessage()))
                .onErrorResume(e -> {
                    log.error("[Research] 启动任务失败, taskId={}", taskId, e);
                    researchBizService.markTaskFailed(taskId, e.getMessage());
                    return Flux.just(ResearchEventVO.builder()
                            .type("error")
                            .data("启动研究任务失败: " + e.getMessage())
                            .build());
                });
    }

    @Operation(summary = "恢复研究任务", description = "用户回答澄清问题、确认计划或提供修改意见，返回SSE事件流")
    @PostMapping(value = "/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResearchEventVO> resumeResearch(@Valid @RequestBody ResearchResumeDTO dto) {
        String taskId = dto.getTaskId();

        // 检查任务是否存在
        ResearchTask task = researchBizService.getTask(taskId);
        if (task == null) {
            return Flux.just(ResearchEventVO.builder()
                    .type("error")
                    .data("任务不存在")
                    .build());
        }

        // 更新用户反馈
        researchBizService.updateUserFeedback(taskId, dto.getResponse());

        // 调用 Python 服务恢复研究，返回 SSE 事件流
        return pythonResearchService.streamResumeResearch(taskId, dto.getResponse())
                .onErrorResume(e -> {
                    log.error("[Research] 恢复任务失败, taskId={}", taskId, e);
                    researchBizService.markTaskFailed(taskId, e.getMessage());
                    return Flux.just(ResearchEventVO.builder()
                            .type("error")
                            .data("恢复研究任务失败: " + e.getMessage())
                            .build());
                });
    }

    @Operation(summary = "停止研究任务", description = "终止正在进行的研究任务")
    @PostMapping("/stop")
    public Mono<JsonData<Void>> stopResearch(@RequestParam String taskId) {
        Long userId = UserContextHolder.getUserId();

        // 验证任务归属
        ResearchTask task = researchBizService.verifyTaskOwnership(taskId, userId);
        if (task == null) {
            return Mono.just(JsonData.buildError("任务不存在或无权限"));
        }

        // 更新任务状态为已停止
        researchBizService.stopTask(taskId);

        // 调用 Python 服务停止研究
        return pythonResearchService.stopResearch(taskId)
                .map(success -> success ? JsonData.<Void>buildSuccess() : JsonData.<Void>buildError("停止研究任务失败"))
                .onErrorResume(e -> {
                    log.error("[Research] 停止任务失败, taskId={}", taskId, e);
                    return Mono.just(JsonData.buildError("停止研究任务失败: " + e.getMessage()));
                });
    }

    @Operation(summary = "获取用户研究历史", description = "分页查询用户的研究历史记录")
    @GetMapping("/history")
    public JsonData<List<ResearchHistoryVO>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getUserId();
        List<ResearchHistoryVO> history = researchBizService.getUserHistory(userId, page, size);
        return JsonData.buildSuccess(history);
    }

    @Operation(summary = "获取研究报告详情", description = "根据任务ID获取研究报告")
    @GetMapping("/report/{taskId}")
    public JsonData<ResearchReportVO> getReport(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        Long userId = UserContextHolder.getUserId();

        ResearchReportVO report = researchBizService.getReport(taskId, userId);
        if (report == null) {
            return JsonData.buildError("报告不存在或无权限");
        }

        return JsonData.buildSuccess(report);
    }
}
