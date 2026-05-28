package com.personblog.ai.controller.CallPythonController;

import com.personblog.ai.BizService.PythonWritingService;
import com.personblog.ai.dto.WritingResumeDTO;
import com.personblog.ai.dto.WritingTaskCreateDTO;
import com.personblog.ai.entity.WritingTask;
import com.personblog.ai.service.IWritingTaskService;
import com.personblog.ai.vo.WritingEventVO;
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
    @Operation(summary = "创建并启动写作任务", description = "在数据库中创建写作任务记录，并调用AI服务开始生成大纲")
    @PostMapping("/create")
    public Mono<JsonData<WritingTaskVO>> createAndStart(@Valid @RequestBody WritingTaskCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        WritingTask task = writingTaskService.createTask(userId, dto.getMessage());
        String taskId = String.valueOf(task.getId());
        log.info("[Writing] 创建任务成功, taskId={}, userId={}", task.getId(), userId);

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
}
