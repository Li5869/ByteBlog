package com.personblog.ai.service;

import com.personblog.ai.entity.WritingPlan;
import com.personblog.ai.entity.WritingTask;
import com.personblog.ai.vo.WritingTaskDetailVO;
import com.personblog.ai.vo.WritingTaskListVO;

import java.util.List;
import java.util.Map;

/**
 * 写作任务服务接口
 *
 * @author LSH
 */
public interface IWritingTaskService {

    WritingTask createTask(Long userId, String userRequest);

    WritingTask getById(Long taskId);

    /**
     * 获取指定用户的所有写作任务列表，按创建时间倒序排列
     */
    List<WritingTaskListVO> listByUserId(Long userId);

    /**
     * 获取任务详情（含最新写作计划，用于前端恢复任务到对应阶段）
     */
    WritingTaskDetailVO getTaskDetail(Long taskId, Long userId);

    /**
     * 删除指定任务（仅限可删除状态的任务）
     *
     * @param taskId 任务ID
     * @param userId 用户ID（用于校验归属）
     * @return true-删除成功, false-任务不存在或状态不允许删除
     */
    boolean deleteTask(Long taskId, Long userId);

    void updateStatus(Long taskId, String status, String currentStep);

    void completeTask(Long taskId, String finalAction);

    void updateArticleId(Long taskId, Long articleId);

    WritingPlan savePlan(Long taskId, Map<String, Object> planData, Integer version, String userFeedback);

    WritingPlan getLatestPlan(Long taskId);

    void updatePlanApprovalStatus(Long planId, String approvalStatus);
}
