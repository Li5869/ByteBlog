package com.personblog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.ai.entity.ResearchReport;
import com.personblog.ai.entity.ResearchTask;
import com.personblog.ai.mapper.ResearchReportMapper;
import com.personblog.ai.mapper.ResearchTaskMapper;
import com.personblog.ai.service.IResearchTaskService;
import com.personblog.ai.vo.ResearchHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 深度研究任务服务实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchTaskServiceImpl implements IResearchTaskService {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResearchTask createTask(String taskId, Long userId, String topic) {
        ResearchTask task = new ResearchTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setTopic(topic);
        task.setStatus("pending");
        task.setIsDeleted(false);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        researchTaskMapper.insert(task);
        log.info("[ResearchTask] 创建任务成功, taskId={}, userId={}, topic={}", taskId, userId, topic);
        return task;
    }

    @Override
    public ResearchTask getByTaskId(String taskId) {
        return researchTaskMapper.selectByTaskId(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String taskId, String status) {
        LambdaUpdateWrapper<ResearchTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResearchTask::getTaskId, taskId)
                .set(ResearchTask::getStatus, status)
                .set(ResearchTask::getUpdatedAt, LocalDateTime.now());
        researchTaskMapper.update(null, updateWrapper);
        log.info("[ResearchTask] 更新任务状态, taskId={}, status={}", taskId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(String taskId, String plan) {
        LambdaUpdateWrapper<ResearchTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResearchTask::getTaskId, taskId)
                .set(ResearchTask::getPlan, plan)
                .set(ResearchTask::getUpdatedAt, LocalDateTime.now());
        researchTaskMapper.update(null, updateWrapper);
        log.info("[ResearchTask] 更新研究计划, taskId={}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClarifiedRequirements(String taskId, String clarifiedRequirements) {
        LambdaUpdateWrapper<ResearchTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResearchTask::getTaskId, taskId)
                .set(ResearchTask::getClarifiedRequirements, clarifiedRequirements)
                .set(ResearchTask::getUpdatedAt, LocalDateTime.now());
        researchTaskMapper.update(null, updateWrapper);
        log.info("[ResearchTask] 更新澄清需求, taskId={}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserFeedback(String taskId, String userFeedback) {
        LambdaUpdateWrapper<ResearchTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResearchTask::getTaskId, taskId)
                .set(ResearchTask::getUserFeedback, userFeedback)
                .set(ResearchTask::getUpdatedAt, LocalDateTime.now());
        researchTaskMapper.update(null, updateWrapper);
        log.info("[ResearchTask] 更新用户反馈, taskId={}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateErrorMsg(String taskId, String errorMsg) {
        LambdaUpdateWrapper<ResearchTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResearchTask::getTaskId, taskId)
                .set(ResearchTask::getErrorMsg, errorMsg)
                .set(ResearchTask::getUpdatedAt, LocalDateTime.now());
        researchTaskMapper.update(null, updateWrapper);
        log.info("[ResearchTask] 更新错误信息, taskId={}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTaskFailed(String taskId, String errorMsg) {
        // 单次数据库操作同时更新状态和错误信息
        LambdaUpdateWrapper<ResearchTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResearchTask::getTaskId, taskId)
                .set(ResearchTask::getStatus, "failed")
                .set(ResearchTask::getErrorMsg, errorMsg)
                .set(ResearchTask::getUpdatedAt, LocalDateTime.now());
        researchTaskMapper.update(null, updateWrapper);
        log.info("[ResearchTask] 标记任务失败, taskId={}, error={}", taskId, errorMsg);
    }

    @Override
    public List<ResearchHistoryVO> getUserHistory(Long userId, int page, int size) {
        // 分页查询用户的研究任务
        Page<ResearchTask> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ResearchTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ResearchTask::getUserId, userId)
                .eq(ResearchTask::getIsDeleted, false)
                .orderByDesc(ResearchTask::getCreatedAt);
        Page<ResearchTask> result = researchTaskMapper.selectPage(pageParam, queryWrapper);

        if (result.getRecords() == null || result.getRecords().isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询关联的报告摘要
        List<String> taskIds = result.getRecords().stream()
                .map(ResearchTask::getTaskId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<ResearchReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.in(ResearchReport::getTaskId, taskIds)
                .eq(ResearchReport::getIsDeleted, false);
        List<ResearchReport> reports = researchReportMapper.selectList(reportWrapper);

        // 构建 taskId -> summary 映射
        var reportSummaryMap = reports.stream()
                .collect(Collectors.toMap(ResearchReport::getTaskId, ResearchReport::getSummary, (a, b) -> a));

        // 组装历史记录
        return result.getRecords().stream().map(task -> {
            ResearchHistoryVO vo = new ResearchHistoryVO();
            vo.setId(task.getId());
            vo.setTaskId(task.getTaskId());
            vo.setTopic(task.getTopic());
            vo.setStatus(task.getStatus());
            vo.setCreatedAt(task.getCreatedAt());
            vo.setReportSummary(reportSummaryMap.get(task.getTaskId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTask(String taskId, Long userId) {
        ResearchTask task = researchTaskMapper.selectByTaskId(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            log.warn("[ResearchTask] 删除任务失败：任务不存在或无权限, taskId={}, userId={}", taskId, userId);
            return false;
        }
        // 校验状态：只有已完成或失败的任务才能删除
        String status = task.getStatus();
        if ("executing".equals(status) || "reporting".equals(status)) {
            log.warn("[ResearchTask] 删除任务失败：任务正在执行中, taskId={}, status={}", taskId, status);
            return false;
        }
        // 逻辑删除任务
        LambdaUpdateWrapper<ResearchTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResearchTask::getTaskId, taskId)
                .set(ResearchTask::getIsDeleted, true)
                .set(ResearchTask::getUpdatedAt, LocalDateTime.now());
        researchTaskMapper.update(null, updateWrapper);
        log.info("[ResearchTask] 删除任务成功, taskId={}, userId={}", taskId, userId);
        return true;
    }
}
