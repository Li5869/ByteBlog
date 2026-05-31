package com.personblog.ai.service;

import com.personblog.ai.entity.WritingDraft;
import com.personblog.ai.vo.WritingDraftVO;

import java.util.Map;

/**
 * 写作草稿服务接口
 *
 * @author LSH
 */
public interface IWritingDraftService {


    /**
     * 根据任务ID获取草稿
     *
     * @param taskId 任务ID
     * @return 草稿实体
     */
    WritingDraftVO getByTaskId(Long taskId);

    /**
     * 保存草稿
     *
     * @param taskId    任务ID
     * @param userId    用户ID
     * @param draftData 草稿数据
     * @return 草稿实体
     */
    WritingDraft saveDraft(Long taskId, Long userId, Map<String, Object> draftData);

    /**
     * 删除草稿
     *
     * @param taskId 任务ID
     */
    void deleteByTaskId(Long taskId);
}
