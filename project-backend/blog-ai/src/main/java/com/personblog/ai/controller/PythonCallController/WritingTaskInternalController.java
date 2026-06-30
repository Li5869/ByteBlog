package com.personblog.ai.controller.PythonCallController;

import com.personblog.ai.dto.WritingTaskUpdateDTO;
import com.personblog.ai.entity.WritingDraft;
import com.personblog.ai.entity.WritingPlan;
import com.personblog.ai.entity.WritingReflection;
import com.personblog.ai.entity.WritingTask;
import com.personblog.ai.service.IWritingDraftService;
import com.personblog.ai.service.IWritingReflectionService;
import com.personblog.ai.service.IWritingTaskService;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 写作任务内部API控制器
 * 供 Python AI 服务调用，用于更新任务状态
 *
 * @author LSH
 */
@Tag(name = "写作任务内部API", description = "供Python AI服务调用的内部接口")
@Slf4j
@RestController
@RequestMapping("/ai/writing/internal")
@RequiredArgsConstructor
public class WritingTaskInternalController {

    private final IWritingTaskService writingTaskService;
    private final IWritingDraftService writingDraftService;
    private final IWritingReflectionService writingReflectionService;

    @Operation(summary = "创建写作任务", description = "SmartAgent调用此接口创建写作任务")
    @PostMapping("/task")
    public JsonData<Long> createTask(@RequestBody Map<String, Object> request) {
        Long userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id").toString()) : null;
        String userRequest = (String) request.get("user_request");
        
        WritingTask task = writingTaskService.createTask(userId, userRequest);
        log.info("[WritingInternal] 创建任务成功, taskId={}, userId={}", task.getId(), userId);
        return JsonData.buildSuccess(task.getId());
    }

    @Operation(summary = "更新任务状态", description = "Python服务调用此接口更新任务状态")
    @PutMapping("/task/{taskId}/status")
    public JsonData<Void> updateStatus(
            @PathVariable Long taskId,
            @RequestBody WritingTaskUpdateDTO dto) {
        writingTaskService.updateStatus(taskId, dto.getStatus(), dto.getCurrentStep());
        return JsonData.buildSuccess();
    }

    @Operation(summary = "保存写作计划", description = "Python服务调用此接口保存计划")
    @PostMapping("/plan")
    public JsonData<Long> savePlan(@RequestBody Map<String, Object> request) {
        Long taskId = Long.parseLong(request.get("task_id").toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> planData = (Map<String, Object>) request.get("plan_data");
        Integer version = request.get("version") != null ? (Integer) request.get("version") : 1;
        String userFeedback = (String) request.get("user_feedback");
        
        WritingPlan plan = writingTaskService.savePlan(taskId, planData, version, userFeedback);
        return JsonData.buildSuccess(plan.getId());
    }

    @Operation(summary = "更新计划审核状态", description = "Python服务调用此接口更新计划审核状态")
    @PutMapping("/plan/{planId}/approval")
    public JsonData<Void> updatePlanApprovalStatus(
            @PathVariable Long planId,
            @RequestParam String approvalStatus) {
        writingTaskService.updatePlanApprovalStatus(planId, approvalStatus);
        return JsonData.buildSuccess();
    }

    @Operation(summary = "获取任务信息", description = "Python服务调用此接口获取任务信息")
    @GetMapping("/task/{taskId}")
    public JsonData<WritingTask> getTask(@PathVariable Long taskId) {
        WritingTask task = writingTaskService.getById(taskId);
        return JsonData.buildSuccess(task);
    }

    @Operation(summary = "获取最新计划", description = "Python服务调用此接口获取最新计划")
    @GetMapping("/plan/latest/{taskId}")
    public JsonData<WritingPlan> getLatestPlan(@PathVariable Long taskId) {
        WritingPlan plan = writingTaskService.getLatestPlan(taskId);
        return JsonData.buildSuccess(plan);
    }

    @Operation(summary = "更新修订次数", description = "Python服务调用此接口更新修订次数")
    @PutMapping("/task/{taskId}/revision")
    public JsonData<Void> updateRevisionCount(
            @PathVariable Long taskId,
            @RequestParam Integer revisionCount) {
        writingTaskService.updateRevisionCount(taskId, revisionCount);
        return JsonData.buildSuccess();
    }

    // ==================== 草稿相关 ====================

    @Operation(summary = "保存写作草稿", description = "Python服务调用此接口保存草稿")
    @PostMapping("/draft")
    public JsonData<Long> saveDraft(@RequestBody Map<String, Object> request) {
        Long taskId = Long.parseLong(request.get("task_id").toString());
        Long userId = request.get("user_id") != null ? Long.parseLong(request.get("user_id").toString()) : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> draftData = (Map<String, Object>) request.get("draft_data");

        WritingDraft draft = writingDraftService.saveDraft(taskId, userId, draftData);
        log.info("[WritingDraft] 保存草稿成功, draftId={}, taskId={}", draft.getId(), taskId);
        return JsonData.buildSuccess(draft.getId());
    }

    // ==================== 评估结果相关 ====================

    @Operation(summary = "保存写作评估结果", description = "Python服务调用此接口保存评估结果")
    @PostMapping("/reflection")
    public JsonData<Long> saveReflection(@RequestBody Map<String, Object> request) {
        Long taskId = Long.parseLong(request.get("task_id").toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> reflectionData = (Map<String, Object>) request.get("reflection_data");

        WritingReflection reflection = writingReflectionService.saveReflection(taskId, reflectionData);
        log.info("[WritingReflection] 保存评估结果成功, reflectionId={}, taskId={}", reflection.getId(), taskId);
        return JsonData.buildSuccess(reflection.getId());
    }
}
