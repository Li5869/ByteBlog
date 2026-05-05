package com.personblog.question.controller.Admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.adminLog.RecordLog;
import com.personblog.common.result.JsonData;
import com.personblog.question.dto.AdminQuestionQueryDTO;
import com.personblog.question.service.IAnswerService;
import com.personblog.question.service.IQuestionService;
import com.personblog.question.vo.AdminQuestionDetailVO;
import com.personblog.question.vo.AdminQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端问答管理控制器
 * 支持问题的列表查询、详情查看、删除和回答删除
 *
 * @author LSH
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "管理端-问答管理", description = "管理后台的问答管理接口")
public class AdminQuestionController {

    private final IQuestionService questionService;
    private final IAnswerService answerService;

    /**
     * 获取问题列表（分页）
     * 支持关键词搜索和状态筛选
     */
    @Operation(summary = "获取问题列表", description = "分页查询问题列表，支持关键词搜索和状态筛选")
    @PostMapping("/questions/list")
    public JsonData<Page<AdminQuestionVO>> getQuestionPage(@RequestBody AdminQuestionQueryDTO dto) {
        Page<AdminQuestionVO> page = questionService.getAdminQuestionPage(dto);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取问题详情（含回答列表）
     */
    @Operation(summary = "获取问题详情", description = "获取指定问题的详细信息和所有回答")
    @GetMapping("/questions/{id}")
    public JsonData<AdminQuestionDetailVO> getQuestionDetail(
            @Parameter(description = "问题ID") @PathVariable Long id) {
        AdminQuestionDetailVO detail = questionService.getAdminQuestionDetail(id);
        return JsonData.buildSuccess(detail);
    }

    /**
     * 删除问题
     */
    @RecordLog(Type = "delete", businessType = "question", description = "删除问题")
    @Operation(summary = "删除问题", description = "管理员删除指定问题")
    @DeleteMapping("/questions/{id}")
    public JsonData<Void> deleteQuestion(
            @Parameter(description = "问题ID") @PathVariable Long id) {
        questionService.deleteQuestionByAdmin(id);
        return JsonData.buildSuccess();
    }

    /**
     * 删除回答
     */
    @RecordLog(Type = "delete", businessType = "answer", description = "删除回答")
    @Operation(summary = "删除回答", description = "管理员删除问题下的指定回答")
    @DeleteMapping("/answers/{id}")
    public JsonData<Void> deleteAnswer(
            @Parameter(description = "回答ID") @PathVariable Long id) {
        answerService.deleteAnswerByAdmin(id);
        return JsonData.buildSuccess();
    }
}
