package com.personblog.ai.controller;

import com.personblog.ai.BizService.PythonWritingService;
import com.personblog.ai.dto.WritingResumeDTO;
import com.personblog.ai.dto.WritingTaskCreateDTO;
import com.personblog.ai.entity.WritingDraft;
import com.personblog.ai.entity.WritingTask;
import com.personblog.ai.service.IWritingDraftService;
import com.personblog.ai.service.IWritingTaskService;
import com.personblog.ai.vo.WritingEventVO;
import com.personblog.ai.vo.WritingTaskDetailVO;
import com.personblog.ai.vo.WritingTaskListVO;
import com.personblog.ai.vo.WritingTaskVO;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI写作智能体控制器
 * 提供写作智能体的完整API接口，包括：
 * 1. 创建并启动写作任务 - 创建任务记录并调用AI服务开始生成大纲
 * 2. 恢复写作任务 - 用户确认大纲后执行
 * 3. 流式获取进度 - SSE实时推送写作进度
 * 
 * @author LSH
 */
@Tag(name = "AI写作智能体", description = "AI智能写作相关接口，支持大纲确认和流式写作")
@Slf4j
@RestController
@RequestMapping("/ai/writing")
@RequiredArgsConstructor
public class WritingAgentController {

    private final PythonWritingService pythonWritingService;
    private final IWritingTaskService writingTaskService;
    private final IWritingDraftService writingDraftService;

    @Operation(summary = "创建并启动写作任务", description = "在数据库中创建写作任务记录，并调用AI服务开始生成大纲")
    @PostMapping("/create")
    public Mono<JsonData<WritingTaskVO>> createAndStart(@Valid @RequestBody WritingTaskCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        WritingTask task = writingTaskService.createTask(userId, dto.getMessage());
        String taskId = String.valueOf(task.getId());
        log.info("[Writing] 创建任务成功, taskId={}, userId={}", task.getId(), userId);

        // 幂等检查命中的已有任务：任务已启动过，直接返回不需要再次调用 Python 服务
        if ("generating_plan".equals(task.getCurrentStep()) && task.getCreatedAt().isBefore(LocalDateTime.now().minusSeconds(2))) {
            log.info("[Writing] 幂等命中，跳过Python服务调用, taskId={}", taskId);
            return Mono.just(JsonData.buildSuccess(WritingTaskVO.builder()
                    .taskId(taskId)
                    .status(task.getStatus())
                    .build()));
        }

        return pythonWritingService.startWriting(taskId, dto.getMessage())
                .map(id -> JsonData.buildSuccess(WritingTaskVO.builder()
                        .taskId(id)
                        .status("planning")
                        .build()))
                .onErrorResume(e -> {
                    log.error("[Writing] 启动任务失败, taskId={}", taskId, e);
                    writingTaskService.updateStatus(task.getId(), "error", null);
                    return Mono.just(JsonData.buildError("启动写作任务失败: " + e.getMessage()));
                });
    }

    @Operation(summary = "恢复写作任务", description = "用户审核大纲后，批准执行或要求修改")
    @PostMapping("/{taskId}/resume")
    public Mono<JsonData<WritingTaskVO>> resumeWriting(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Valid @RequestBody WritingResumeDTO dto) {
        
        return pythonWritingService.resumeWriting(taskId, dto.getAction(), dto.getFeedback())
                .map(status -> JsonData.buildSuccess(WritingTaskVO.builder()
                        .taskId(taskId)
                        .status(status)
                        .build()))
                .onErrorResume(e -> {
                    log.error("恢复写作任务失败", e);
                    return Mono.just(JsonData.buildError("恢复写作任务失败: " + e.getMessage()));
                });
    }

    @Operation(summary = "流式获取写作进度", description = "通过SSE实时返回写作各阶段的进度事件，包括大纲生成、内容写作、质量评估等")
    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WritingEventVO> streamWriting(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        return pythonWritingService.streamWriting(taskId);
    }

    // ==================== 任务列表与删除 ====================

    @Operation(summary = "获取我的写作任务列表", description = "获取当前用户的所有写作任务，按创建时间倒序排列")
    @GetMapping("/tasks")
    public JsonData<List<WritingTaskListVO>> getMyTasks() {
        Long userId = UserContextHolder.getUserId();
        List<WritingTaskListVO> taskList = writingTaskService.listByUserId(userId);
        log.info("[Writing] 查询任务列表成功, userId={}, count={}", userId, taskList.size());
        return JsonData.buildSuccess(taskList);
    }

    @Operation(summary = "获取任务详情", description = "获取指定任务的详细信息（含最新写作计划），用于前端恢复任务到对应阶段")
    @GetMapping("/{taskId}/detail")
    public JsonData<WritingTaskDetailVO> getTaskDetail(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = UserContextHolder.getUserId();
        WritingTaskDetailVO detail = writingTaskService.getTaskDetail(taskId, userId);
        if (detail == null) {
            return JsonData.buildError("任务不存在或无权访问");
        }
        return JsonData.buildSuccess(detail);
    }

    @Operation(summary = "删除写作任务", description = "删除指定写作任务（仅限已结束或失败的任务）")
    @DeleteMapping("/{taskId}")
    public JsonData<Void> deleteTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = UserContextHolder.getUserId();
        boolean success = writingTaskService.deleteTask(taskId, userId);
        if (success) {
            log.info("[Writing] 删除任务成功, taskId={}, userId={}", taskId, userId);
            return JsonData.buildSuccess();
        } else {
            return JsonData.buildError("删除任务失败：任务不存在或当前状态不允许删除");
        }
    }

    // ==================== 草稿管理 ====================

    @Operation(summary = "获取草稿", description = "根据任务ID获取写作草稿")
    @GetMapping("/{taskId}/draft")
    public JsonData<WritingDraft> getDraftByTaskId(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        WritingDraft draft = writingDraftService.getByTaskId(taskId);
        return JsonData.buildSuccess(draft);
    }
}
