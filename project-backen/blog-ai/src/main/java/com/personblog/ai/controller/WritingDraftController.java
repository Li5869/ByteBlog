package com.personblog.ai.controller;

import com.personblog.ai.entity.WritingDraft;
import com.personblog.ai.service.IWritingDraftService;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ai写作草稿管理 Controller
 *
 * @author LSH
 */
@Tag(name = "AI写作草稿管理", description = "AI写作草稿管理接口")
@Slf4j
@RestController
@RequestMapping("/ai/writing")
@RequiredArgsConstructor
public class WritingDraftController {
    private final IWritingDraftService writingDraftService;
    @Operation(summary = "获取草稿", description = "根据任务ID获取写作草稿")
    @GetMapping("/{taskId}/draft")
    public JsonData<WritingDraft> getDraftByTaskId(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        WritingDraft draft = writingDraftService.getByTaskId(taskId);
        return JsonData.buildSuccess(draft);
    }
}
