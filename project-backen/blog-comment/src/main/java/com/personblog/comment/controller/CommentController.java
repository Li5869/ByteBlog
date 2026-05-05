package com.personblog.comment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.comment.dto.CommentCreateDTO;
import com.personblog.comment.service.ICommentService;
import com.personblog.comment.vo.CommentRemoveVO;
import com.personblog.comment.vo.CommentVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 评论控制器
 *
 * @author LSH
 */
@Tag(name = "评论接口", description = "评论相关接口")
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final ICommentService commentService;

    /**
     * 获取文章评论列表
     * 支持分页，包含回复信息
     *
     * @param articleId 文章ID
     * @param current   当前页码
     * @param size      每页数量
     * @return 评论分页列表
     */
    @Operation(summary = "获取文章评论列表", description = "获取指定文章的评论列表，支持分页")
    @GetMapping("/comments/article/{articleId}")
    public JsonData<Page<CommentVO>> getCommentPage(
            @Parameter(description = "文章ID")
            @PathVariable Long articleId,
            @Parameter(description = "当前页码，默认1")
            @RequestParam(required = false) Integer current,
            @Parameter(description = "每页数量，默认10，最大50")
            @RequestParam(required = false) Integer size) {
        Page<CommentVO> page = commentService.getCommentPage(articleId, current, size);
        return JsonData.buildSuccess(page);
    }

    /**
     * 发布评论
     * 用户对文章发表评论或回复其他评论
     *
     * @param dto 评论请求参数
     * @return 新创建的评论信息
     */
    @Operation(summary = "发布评论", description = "用户对文章发表评论或回复其他评论")
    @PostMapping("/comments")
    public JsonData<CommentVO> createComment(@Valid @RequestBody CommentCreateDTO dto) {
        CommentVO vo = commentService.createComment(dto);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 删除评论
     * 用户删除自己发表的评论
     *
     * @param id 评论ID
     */
    @Operation(summary = "删除评论", description = "用户删除自己发表的评论")
    @DeleteMapping("/comments/{id}")
    public JsonData<CommentRemoveVO> deleteComment(
            @Parameter(description = "评论ID")
            @PathVariable Long id) {
        CommentRemoveVO vo = commentService.deleteComment(id);
        return JsonData.buildSuccess(vo);
    }


    @GetMapping("/comments/isLikes")
    @Operation(summary = "获取评论是否点赞", description = "根据评论id获取当前用户已经点赞的评论id")
    public JsonData<Set<Long>> getBatchLike(@RequestParam List<Long> targetIds, @RequestParam String targetType) {
        Set<Long> res = commentService.getIsLikes(targetIds, targetType);
        return JsonData.buildSuccess(res);
    }
}
