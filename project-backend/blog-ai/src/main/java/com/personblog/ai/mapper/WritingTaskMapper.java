package com.personblog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.ai.entity.WritingTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 写作任务 Mapper
 *
 * @author LSH
 */
@Mapper
public interface WritingTaskMapper extends BaseMapper<WritingTask> {
}
