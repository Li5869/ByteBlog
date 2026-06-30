package com.personblog.ai.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.personblog.ai.entity.WritingPlan;
import com.personblog.ai.entity.WritingTask;
import com.personblog.ai.mapper.WritingPlanMapper;
import com.personblog.ai.mapper.WritingTaskMapper;
import com.personblog.ai.service.IWritingDraftService;
import com.personblog.ai.service.IWritingTaskService;
import com.personblog.ai.vo.WritingTaskDetailVO;
import com.personblog.ai.vo.WritingTaskListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 写作任务服务实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WritingTaskServiceImpl implements IWritingTaskService {

    private final WritingTaskMapper writingTaskMapper;
    private final WritingPlanMapper writingPlanMapper;
    private final IWritingDraftService draftService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WritingTask createTask(Long userId, String userRequest) {
        // 幂等性检查：如果用户最近10秒内已创建过 planning 状态的任务，直接返回该任务
        LocalDateTime tenSecondsAgo = LocalDateTime.now().minusSeconds(10);
        LambdaQueryWrapper<WritingTask> wrapper = new LambdaQueryWrapper<WritingTask>()
                .eq(WritingTask::getUserId, userId)
                .eq(WritingTask::getStatus, "planning")
                .ge(WritingTask::getCreatedAt, tenSecondsAgo)
                .orderByDesc(WritingTask::getCreatedAt)
                .last("LIMIT 1");
        WritingTask existingTask = writingTaskMapper.selectOne(wrapper);
        if (existingTask != null) {
            log.info("[WritingTask] 命中幂等检查，返回已有任务, taskId={}, userId={}", existingTask.getId(), userId);
            return existingTask;
        }

        WritingTask task = new WritingTask();
        task.setUserId(userId);
        task.setUserRequest(userRequest);
        task.setStatus("planning");
        task.setCurrentStep("generating_plan");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        writingTaskMapper.insert(task);
        log.info("[WritingTask] 创建任务成功, taskId={}, userId={}", task.getId(), userId);
        return task;
    }

    @Override
    public WritingTask getById(Long taskId) {
        return writingTaskMapper.selectById(taskId);
    }

    @Override
    public WritingTaskDetailVO getTaskDetail(Long taskId, Long userId) {
        // 查询任务并校验归属
        WritingTask task = writingTaskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            log.warn("[WritingTask] 获取详情失败：任务不存在或无权限, taskId={}, userId={}", taskId, userId);
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        WritingTaskDetailVO.WritingTaskDetailVOBuilder builder = WritingTaskDetailVO.builder()
                .id(task.getId())
                .userRequest(task.getUserRequest())
                .status(task.getStatus())
                .currentStep(task.getCurrentStep())
                .revisionCount(task.getRevisionCount() != null ? task.getRevisionCount() : 0)
                .articleId(task.getArticleId())
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().format(formatter) : null)
                .finalAction(task.getFinalAction());

        // 查询最新计划
        WritingPlan plan = getLatestPlan(taskId);
        if (plan != null) {
            builder.topic(plan.getTopic())
                    .targetAudience(plan.getTargetAudience())
                    .writingStyle(plan.getWritingStyle())
                    .estimatedLength(plan.getEstimatedLength())
                    .keyPoints(parseJsonList(plan.getKeyPoints()))
                    .structure(parseJsonList(plan.getStructure()))
                    .referenceKeywords(parseJsonList(plan.getReferenceKeywords()));
        }

        return builder.build();
    }

    /**
     * 解析 JSON 字符串为 List<String>
     */
    private List<String> parseJsonList(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(jsonStr, String.class);
        } catch (Exception e) {
            log.warn("[WritingTask] JSON列表解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<WritingTaskListVO> listByUserId(Long userId) {
        // 按创建时间倒序查询当前用户的所有任务
        LambdaQueryWrapper<WritingTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WritingTask::getUserId, userId)
                .orderByDesc(WritingTask::getCreatedAt);
        List<WritingTask> tasks = writingTaskMapper.selectList(queryWrapper);
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return tasks.stream().map(task -> WritingTaskListVO.builder()
                .id(task.getId())
                .userRequest(task.getUserRequest())
                .status(task.getStatus())
                .currentStep(task.getCurrentStep())
                .revisionCount(task.getRevisionCount() != null ? task.getRevisionCount() : 0)
                .articleId(task.getArticleId())
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().format(formatter) : null)
                .completedAt(task.getCompletedAt() != null ? task.getCompletedAt().format(formatter) : null)
                .errorMessage(null)
                .finalAction(task.getFinalAction())
                .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTask(Long taskId, Long userId) {
        // 查询任务是否存在且属于当前用户
        WritingTask task = writingTaskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            log.warn("[WritingTask] 删除任务失败：任务不存在或无权限, taskId={}, userId={}", taskId, userId);
            return false;
        }
        // 校验状态：只有已结束或失败的任务才能删除
        String status = task.getStatus();
        if ("planning".equals(status) || "executing".equals(status) || "reflecting".equals(status)) {
            log.warn("[WritingTask] 删除任务失败：任务正在执行中, taskId={}, status={}", taskId, status);
            return false;
        }
        // 同时删除关联的草稿
        draftService.deleteByTaskId(taskId);
        writingTaskMapper.deleteById(taskId);
        log.info("[WritingTask] 删除任务成功, taskId={}, userId={}", taskId, userId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long taskId, String status, String currentStep) {
        LambdaUpdateWrapper<WritingTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WritingTask::getId, taskId)
                .set(WritingTask::getStatus, status)
                .set(WritingTask::getUpdatedAt, LocalDateTime.now());
        if (currentStep != null) {
            updateWrapper.set(WritingTask::getCurrentStep, currentStep);
        }
        writingTaskMapper.update(null, updateWrapper);
        log.info("[WritingTask] 更新任务状态, taskId={}, status={}, step={}", taskId, status, currentStep);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, String finalAction) {
        LambdaUpdateWrapper<WritingTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WritingTask::getId, taskId)
                .set(WritingTask::getStatus, "finalized")
                .set(WritingTask::getCurrentStep, "completed")
                .set(WritingTask::getFinalAction, finalAction)
                .set(WritingTask::getCompletedAt, LocalDateTime.now())
                .set(WritingTask::getUpdatedAt, LocalDateTime.now());
        writingTaskMapper.update(null, updateWrapper);
        //用户完成发布或者存草稿后后删除草稿
        draftService.deleteByTaskId(taskId);
        log.info("[WritingTask] 完成任务, taskId={}, action={}", taskId, finalAction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleId(Long taskId, Long articleId) {
        LambdaUpdateWrapper<WritingTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WritingTask::getId, taskId)
                .set(WritingTask::getArticleId, articleId)
                .set(WritingTask::getUpdatedAt, LocalDateTime.now());
        writingTaskMapper.update(null, updateWrapper);
        log.info("[WritingTask] 更新文章ID, taskId={}, articleId={}", taskId, articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRevisionCount(Long taskId, Integer revisionCount) {
        LambdaUpdateWrapper<WritingTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WritingTask::getId, taskId)
                .set(WritingTask::getRevisionCount, revisionCount)
                .set(WritingTask::getUpdatedAt, LocalDateTime.now());
        writingTaskMapper.update(null, updateWrapper);
        log.info("[WritingTask] 更新修订次数, taskId={}, revisionCount={}", taskId, revisionCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public WritingPlan savePlan(Long taskId, Map<String, Object> planData, Integer version, String userFeedback) {
        WritingPlan plan = new WritingPlan();
        plan.setTaskId(taskId);
        plan.setVersion(version != null ? version : 1);
        plan.setTopic((String) planData.get("topic"));
        plan.setTargetAudience((String) planData.get("target_audience"));
        plan.setKeyPoints(toJsonString((List<String>) planData.get("key_points")));
        plan.setWritingStyle((String) planData.get("writing_style"));
        plan.setEstimatedLength((String) planData.get("estimated_length"));
        plan.setReferenceKeywords(toJsonString((List<String>) planData.get("reference_keywords")));
        plan.setStructure(toJsonString((List<String>) planData.get("structure")));
        plan.setApprovalStatus("pending");
        plan.setUserFeedback(userFeedback);
        plan.setCreatedAt(LocalDateTime.now());
        writingPlanMapper.insert(plan);
        log.info("[WritingPlan] 保存计划成功, planId={}, taskId={}", plan.getId(), taskId);
        return plan;
    }

    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JSONUtil.toJsonStr(list);
    }

    @Override
    public WritingPlan getLatestPlan(Long taskId) {
        LambdaQueryWrapper<WritingPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WritingPlan::getTaskId, taskId)
                .orderByDesc(WritingPlan::getVersion)
                .last("LIMIT 1");
        return writingPlanMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlanApprovalStatus(Long planId, String approvalStatus) {
        LambdaUpdateWrapper<WritingPlan> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WritingPlan::getId, planId)
                .set(WritingPlan::getApprovalStatus, approvalStatus);
        writingPlanMapper.update(null, updateWrapper);
        log.info("[WritingPlan] 更新计划审核状态, planId={}, status={}", planId, approvalStatus);
    }
}
