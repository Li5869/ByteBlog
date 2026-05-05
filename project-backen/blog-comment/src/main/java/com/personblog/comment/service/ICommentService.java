package com.personblog.comment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.comment.dto.AdminCommentQueryDTO;
import com.personblog.comment.dto.CommentCreateDTO;
import com.personblog.comment.entity.Comment;
import com.personblog.comment.vo.AdminCommentDetailVO;
import com.personblog.comment.vo.AdminCommentVO;
import com.personblog.comment.vo.CommentRemoveVO;
import com.personblog.comment.vo.CommentVO;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 评论表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface ICommentService extends IService<Comment> {

    /**
     * 获取文章评论列表（分页）
     * @param articleId 文章ID
     * @param current 当前页码
     * @param size 每页数量
     * @return 评论分页列表
     */
    Page<CommentVO> getCommentPage(Long articleId, Integer current, Integer size);

    Set<Long> getIsLikes(List<Long> targetIds,String targetType);

    /**
     * 发布评论
     * @param dto 评论请求参数
     * @return 新创建的评论信息
     */
    CommentVO createComment(CommentCreateDTO dto);

    /**
     * 删除评论
     * @param id 评论ID
     */
    CommentRemoveVO deleteComment(Long id);

    // ==================== 管理端接口 ====================

    /**
     * 管理端 - 分页查询评论列表
     */
    Page<AdminCommentVO> getAdminCommentPage(AdminCommentQueryDTO dto);

    /**
     * 管理端 - 获取评论详情（含回复列表）
     */
    AdminCommentDetailVO getAdminCommentDetail(Long id);

    /**
     * 管理端 - 审核通过
     */
    void approveCommentByAdmin(Long id);

    /**
     * 管理端 - 审核拒绝
     */
    void rejectCommentByAdmin(Long id);

    /**
     * 管理端 - 删除评论
     */
    void deleteCommentByAdmin(Long id);
}
