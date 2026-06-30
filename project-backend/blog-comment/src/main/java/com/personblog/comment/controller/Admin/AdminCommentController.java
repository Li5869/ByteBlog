package com.personblog.comment.controller.Admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.admin.aspect.RecordLog;
import com.personblog.comment.dto.AdminCommentQueryDTO;
import com.personblog.comment.service.ICommentService;
import com.personblog.comment.vo.AdminCommentDetailVO;
import com.personblog.comment.vo.AdminCommentVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端评论管理控制器
 * 支持评论的列表查询、详情查看、审核和删除
 *
 * @author LSH
 */
@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Tag(name = "管理端-评论管理", description = "管理后台的评论管理接口")
public class AdminCommentController {

    private final ICommentService commentService;

    /**
     * 获取评论列表（分页）
     * 支持关键词搜索、审核状态筛选
     */
    @Operation(summary = "获取评论列表", description = "分页查询评论列表，支持关键词搜索和状态筛选")
    @PostMapping("/list")
    public JsonData<Page<AdminCommentVO>> getCommentPage(@RequestBody AdminCommentQueryDTO dto) {
        Page<AdminCommentVO> page = commentService.getAdminCommentPage(dto);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取评论详情（含回复列表）
     */
    @Operation(summary = "获取评论详情", description = "获取单条评论的完整详情，含回复列表")
    @GetMapping("/{id}")
    public JsonData<AdminCommentDetailVO> getCommentDetail(
            @Parameter(description = "评论ID") @PathVariable Long id) {
        AdminCommentDetailVO detail = commentService.getAdminCommentDetail(id);
        return JsonData.buildSuccess(detail);
    }

    /**
     * 审核通过
     */
    @RecordLog(Type = "review", businessType = "comment", description = "审核通过评论")
    @Operation(summary = "审核通过", description = "管理员审核通过评论")
    @PutMapping("/{id}/approve")
    public JsonData<Void> approveComment(
            @Parameter(description = "评论ID") @PathVariable Long id) {
        commentService.approveCommentByAdmin(id);
        return JsonData.buildSuccess();
    }

    /**
     * 审核拒绝
     */
    @RecordLog(Type = "review", businessType = "comment", description = "审核拒绝评论")
    @Operation(summary = "审核拒绝", description = "管理员审核拒绝评论")
    @PutMapping("/{id}/reject")
    public JsonData<Void> rejectComment(
            @Parameter(description = "评论ID") @PathVariable Long id) {
        commentService.rejectCommentByAdmin(id);
        return JsonData.buildSuccess();
    }

    /**
     * 删除评论
     */
    @RecordLog(Type = "delete", businessType = "comment", description = "删除评论")
    @Operation(summary = "删除评论", description = "管理员删除指定评论")
    @DeleteMapping("/{id}")
    public JsonData<Void> deleteComment(
            @Parameter(description = "评论ID") @PathVariable Long id) {
        commentService.deleteCommentByAdmin(id);
        return JsonData.buildSuccess();
    }
}
