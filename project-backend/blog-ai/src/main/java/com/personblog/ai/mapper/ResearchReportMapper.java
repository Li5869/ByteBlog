package com.personblog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.ai.entity.ResearchReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 深度研究报告 Mapper
 *
 * @author LSH
 */
@Mapper
public interface ResearchReportMapper extends BaseMapper<ResearchReport> {

    /**
     * 根据任务UUID查询报告
     *
     * @param taskId 任务UUID
     * @return 报告信息
     */
    @Select("SELECT * FROM tb_research_report WHERE task_id = #{taskId} AND is_deleted = false")
    ResearchReport selectByTaskId(@Param("taskId") String taskId);
}
