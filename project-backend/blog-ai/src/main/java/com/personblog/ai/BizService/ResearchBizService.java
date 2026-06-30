package com.personblog.ai.BizService;

import com.personblog.ai.entity.ResearchReport;
import com.personblog.ai.entity.ResearchTask;
import com.personblog.ai.service.IResearchReportService;
import com.personblog.ai.service.IResearchTaskService;
import com.personblog.ai.vo.ResearchHistoryVO;
import com.personblog.ai.vo.ResearchReportVO;
import com.personblog.ai.vo.ResearchTaskVO;
import com.personblog.common.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 深度研究业务服务
 * 封装研究相关的业务逻辑，供 Controller 调用
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchBizService {

    private final IResearchTaskService researchTaskService;
    private final IResearchReportService researchReportService;
    private final AliOssUtil aliOssUtil;

    /**
     * 创建研究任务
     *
     * @param taskId  前端生成的任务UUID
     * @param userId  用户ID
     * @param message 研究需求
     * @return 任务 VO
     */
    @Transactional(rollbackFor = Exception.class)
    public ResearchTaskVO createTask(String taskId, Long userId, String message) {
        ResearchTask task = researchTaskService.createTask(taskId, userId, message);
        log.info("[Research] 创建任务成功, taskId={}, userId={}", taskId, userId);

        return ResearchTaskVO.builder()
                .id(task.getId())
                .taskId(taskId)
                .topic(message)
                .status("pending")
                .createdAt(task.getCreatedAt())
                .build();
    }

    /**
     * 标记任务失败
     *
     * @param taskId   任务UUID
     * @param errorMsg 错误信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void markTaskFailed(String taskId, String errorMsg) {
        // 使用单次数据库操作同时更新状态和错误信息
        researchTaskService.markTaskFailed(taskId, errorMsg);
    }

    /**
     * 验证任务是否存在
     *
     * @param taskId 任务UUID
     * @return 任务实体，不存在返回 null
     */
    public ResearchTask getTask(String taskId) {
        return researchTaskService.getByTaskId(taskId);
    }

    /**
     * 更新用户反馈
     *
     * @param taskId   任务UUID
     * @param feedback 用户反馈
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserFeedback(String taskId, String feedback) {
        researchTaskService.updateUserFeedback(taskId, feedback);
    }

    /**
     * 验证任务归属
     *
     * @param taskId 任务UUID
     * @param userId 用户ID
     * @return 任务实体，不匹配返回 null
     */
    public ResearchTask verifyTaskOwnership(String taskId, Long userId) {
        ResearchTask task = researchTaskService.getByTaskId(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            return null;
        }
        return task;
    }

    /**
     * 停止研究任务
     *
     * @param taskId 任务UUID
     */
    @Transactional(rollbackFor = Exception.class)
    public void stopTask(String taskId) {
        researchTaskService.updateStatus(taskId, "stopped");
    }

    /**
     * 获取用户研究历史
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 历史记录列表
     */
    public List<ResearchHistoryVO> getUserHistory(Long userId, int page, int size) {
        return researchTaskService.getUserHistory(userId, page, size);
    }

    /**
     * 获取研究报告
     *
     * @param taskId 任务UUID
     * @param userId 用户ID
     * @return 报告 VO，无权限或不存在返回 null
     */
    public ResearchReportVO getReport(String taskId, Long userId) {
        ResearchReport report = researchReportService.getByTaskId(taskId);
        if (report == null || !userId.equals(report.getUserId())) {
            return null;
        }

        ResearchReportVO vo = ResearchReportVO.builder()
                .topic(report.getTopic())
                .reportUrl(report.getReportUrl())
                .summary(report.getSummary())
                .build();

        // 解析 JSON 字段
        try {
            if (report.getKeyFindings() != null) {
                vo.setKeyFindings(cn.hutool.json.JSONUtil.toList(report.getKeyFindings(), String.class));
            }
            if (report.getSources() != null) {
                vo.setSources(cn.hutool.json.JSONUtil.toList(report.getSources(), ResearchReportVO.SourceItem.class));
            }
        } catch (Exception e) {
            log.warn("[Research] 解析报告JSON字段失败: {}", e.getMessage());
        }

        return vo;
    }

    /**
     * 保存研究报告（内部回调用）
     * 包括上传 OSS 和数据库持久化
     *
     * @param taskId      任务UUID
     * @param content     报告内容（Markdown）
     * @param summary     摘要
     * @param keyFindings 关键发现
     * @param sources     引用来源
     * @return 报告 OSS URL
     */
    @Transactional(rollbackFor = Exception.class)
    public String saveReport(String taskId, String content, String summary,
                            List<String> keyFindings, List<java.util.Map<String, String>> sources) {
        // 查询任务信息
        ResearchTask task = researchTaskService.getByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 上传 Markdown 内容到 OSS
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".md";
        String objectName = "research/" + datePath + "/" + fileName;

        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        String reportUrl = aliOssUtil.upload(contentBytes, objectName);
        log.info("[Research] 报告上传OSS成功, taskId={}, url={}", taskId, reportUrl);

        // 保存报告
        ResearchReport report = new ResearchReport();
        report.setTaskId(taskId);
        report.setUserId(task.getUserId());
        report.setTopic(task.getTopic());
        report.setReportUrl(reportUrl);
        report.setSummary(summary);
        report.setKeyFindings(cn.hutool.json.JSONUtil.toJsonStr(keyFindings));
        report.setSources(cn.hutool.json.JSONUtil.toJsonStr(sources));

        researchReportService.createReport(report);
        log.info("[Research] 保存报告成功, taskId={}", taskId);

        // 更新任务状态为已完成
        researchTaskService.updateStatus(taskId, "completed");

        return reportUrl;
    }

    /**
     * 更新任务状态和计划（内部回调用）
     *
     * @param taskId 任务UUID
     * @param status 新状态
     * @param plan   研究计划
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskAndPlan(String taskId, String status, String plan) {
        if (status != null) {
            researchTaskService.updateStatus(taskId, status);
        }
        if (plan != null) {
            researchTaskService.updatePlan(taskId, plan);
        }
        log.info("[Research] 更新任务成功, taskId={}, status={}", taskId, status);
    }

}
