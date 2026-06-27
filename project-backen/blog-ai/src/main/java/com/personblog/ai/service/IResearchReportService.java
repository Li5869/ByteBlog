package com.personblog.ai.service;

import com.personblog.ai.entity.ResearchReport;

/**
 * 深度研究报告服务接口
 *
 * @author LSH
 */
public interface IResearchReportService {

    /**
     * 创建研究报告
     *
     * @param report 报告实体
     * @return 创建后的报告实体
     */
    ResearchReport createReport(ResearchReport report);

    /**
     * 根据任务UUID查询报告
     *
     * @param taskId 任务UUID
     * @return 报告实体，不存在返回null
     */
    ResearchReport getByTaskId(String taskId);
}
