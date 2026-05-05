package com.personblog.ai.service;

import com.personblog.ai.entity.WritingReflection;

import java.util.Map;

/**
 * 写作反思评价服务接口
 *
 * @author LSH
 */
public interface IWritingReflectionService {

    /**
     * 根据ID获取反思评价
     *
     * @param id 反思评价ID
     * @return 反思评价实体
     */
    WritingReflection getById(Long id);

    /**
     * 根据任务ID获取反思评价
     *
     * @param taskId 任务ID
     * @return 反思评价实体
     */
    WritingReflection getByTaskId(Long taskId);

    /**
     * 保存反思评价
     *
     * @param taskId       任务ID
     * @param reflectionData 反思评价数据
     * @return 反思评价实体
     */
    WritingReflection saveReflection(Long taskId, Map<String, Object> reflectionData);
}
