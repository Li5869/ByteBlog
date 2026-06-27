package com.personblog.ai.controller.PythonCallController;

import com.personblog.ai.BizService.ResearchBizService;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 深度研究内部API控制器
 * 供 Python AI 服务调用，用于持久化研究报告和更新任务状态
 *
 * @author LSH
 */
@Tag(name = "深度研究内部API", description = "供Python AI服务调用的内部接口")
@Slf4j
@RestController
@RequestMapping("/ai/research/internal")
@RequiredArgsConstructor
public class ResearchInternalController {

    private final ResearchBizService researchBizService;

    @Operation(summary = "保存研究报告", description = "Python服务调用此接口保存研究报告")
    @PostMapping("/report")
    public JsonData<String> saveReport(@RequestBody Map<String, Object> request) {
        String taskId = (String) request.get("taskId");
        String content = (String) request.get("content");
        String summary = (String) request.get("summary");
        @SuppressWarnings("unchecked")
        List<String> keyFindings = (List<String>) request.get("keyFindings");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> sources = (List<Map<String, String>>) request.get("sources");

        try {
            String reportUrl = researchBizService.saveReport(taskId, content, summary, keyFindings, sources);
            return JsonData.buildSuccess(reportUrl);
        } catch (IllegalArgumentException e) {
            log.warn("[ResearchInternal] {}", e.getMessage());
            return JsonData.buildError(e.getMessage());
        }
    }

    @Operation(summary = "更新任务状态和计划", description = "Python服务调用此接口更新任务状态和研究计划")
    @PostMapping("/task/update")
    public JsonData<Void> updateTask(@RequestBody Map<String, String> request) {
        String taskId = request.get("taskId");
        String status = request.get("status");
        String plan = request.get("plan");

        researchBizService.updateTaskAndPlan(taskId, status, plan);
        return JsonData.buildSuccess();
    }

    @Operation(summary = "更新错误信息", description = "Python服务调用此接口记录错误信息")
    @PostMapping("/task/error")
    public JsonData<Void> updateError(@RequestBody Map<String, String> request) {
        String taskId = request.get("taskId");
        String errorMsg = request.get("errorMsg");

        researchBizService.markTaskFailed(taskId, errorMsg);
        return JsonData.buildSuccess();
    }
}
