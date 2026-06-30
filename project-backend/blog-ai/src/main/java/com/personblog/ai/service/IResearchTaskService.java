package com.personblog.ai.service;

import com.personblog.ai.entity.ResearchTask;
import com.personblog.ai.vo.ResearchHistoryVO;

import java.util.List;

/**
 * 深度研究任务服务接口
 *
 * @author LSH
 */
public interface IResearchTaskService {

    /**
     * 创建研究任务
     *
     * @param taskId 前端生成的任务UUID
     * @param userId 用户ID
     * @param topic  研究主题
     * @return 创建的任务实体
     */
    ResearchTask createTask(String taskId, Long userId, String topic);

    /**
     * 根据任务UUID查询任务
     *
     * @param taskId 任务UUID
     * @return 任务实体，不存在返回null
     */
    ResearchTask getByTaskId(String taskId);

    /**
     * 更新任务状态
     *
     * @param taskId 任务UUID
     * @param status 新状态
     */
    void updateStatus(String taskId, String status);

    /**
     * 更新研究计划
     *
     * @param taskId 任务UUID
     * @param plan   计划JSON字符串
     */
    void updatePlan(String taskId, String plan);

    /**
     * 更新澄清后的需求
     *
     * @param taskId              任务UUID
     * @param clarifiedRequirements 澄清后的需求
     */
    void updateClarifiedRequirements(String taskId, String clarifiedRequirements);

    /**
     * 更新用户反馈
     *
     * @param taskId       任务UUID
     * @param userFeedback 用户反馈
     */
    void updateUserFeedback(String taskId, String userFeedback);

    /**
     * 更新错误信息
     *
     * @param taskId  任务UUID
     * @param errorMsg 错误信息
     */
    void updateErrorMsg(String taskId, String errorMsg);

    /**
     * 标记任务失败并记录错误信息（单次数据库操作）
     *
     * @param taskId   任务UUID
     * @param errorMsg 错误信息
     */
    void markTaskFailed(String taskId, String errorMsg);

    /**
     * 获取用户研究历史（分页）
     *
     * @param userId 用户ID
     * @param page   页码（从1开始）
     * @param size   每页大小
     * @return 历史记录列表
     */
    List<ResearchHistoryVO> getUserHistory(Long userId, int page, int size);

    /**
     * 删除研究任务（逻辑删除）
     *
     * @param taskId 任务UUID
     * @param userId 用户ID（校验归属）
     * @return 是否删除成功
     */
    boolean deleteTask(String taskId, Long userId);
}
