package com.personblog.ai.service.impl;

import com.personblog.ai.entity.ResearchReport;
import com.personblog.ai.mapper.ResearchReportMapper;
import com.personblog.ai.service.IResearchReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 深度研究报告服务实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchReportServiceImpl implements IResearchReportService {

    private final ResearchReportMapper researchReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResearchReport createReport(ResearchReport report) {
        report.setIsDeleted(false);
        report.setCreatedAt(LocalDateTime.now());
        researchReportMapper.insert(report);
        log.info("[ResearchReport] 创建报告成功, taskId={}, topic={}", report.getTaskId(), report.getTopic());
        return report;
    }

    @Override
    public ResearchReport getByTaskId(String taskId) {
        return researchReportMapper.selectByTaskId(taskId);
    }
}
