package com.personblog.ai.controller;

import com.personblog.ai.BizService.ContentModerationService;
import com.personblog.ai.dto.ContentModerationDTO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容审核接口
 *
 * @author LSH
 */
@Tag(name = "内容审核接口", description = "AI内容审核功能")
@RestController
@RequestMapping("/ai/moderation")
@RequiredArgsConstructor
public class ContentModerationController {

    private final ContentModerationService contentModerationService;

    @Operation(summary = "审核内容")
    @PostMapping("/check")
    public JsonData<Void> moderate(@Valid @RequestBody ContentModerationDTO dto) {
        contentModerationService.moderate(dto);
        return JsonData.buildSuccess();
    }
}