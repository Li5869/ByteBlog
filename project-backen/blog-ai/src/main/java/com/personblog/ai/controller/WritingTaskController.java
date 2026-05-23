package com.personblog.ai.controller;

import com.personblog.ai.service.IWritingTaskService;
import com.personblog.ai.vo.WritingTaskDetailVO;
import com.personblog.ai.vo.WritingTaskListVO;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ai写作任务管理 Controller
 *
 * @author LSH
 */
@Tag(name = "AI写作任务管理", description = "AI写作任务管理接口")
@Slf4j
@RestController
@RequestMapping("/ai/writing")
@RequiredArgsConstructor
public class WritingTaskController {
    private final IWritingTaskService writingTaskService;

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
}
