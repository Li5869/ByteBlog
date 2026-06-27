package com.personblog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.ai.entity.ResearchTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 深度研究任务 Mapper
 *
 * @author LSH
 */
@Mapper
public interface ResearchTaskMapper extends BaseMapper<ResearchTask> {

    /**
     * 根据任务UUID查询任务
     *
     * @param taskId 任务UUID
     * @return 任务信息
     */
    @Select("SELECT * FROM tb_research_task WHERE task_id = #{taskId} AND is_deleted = false")
    ResearchTask selectByTaskId(@Param("taskId") String taskId);
}
