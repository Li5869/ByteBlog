package com.personblog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.ai.entity.WritingDraft;
import com.personblog.ai.mapper.WritingDraftMapper;
import com.personblog.ai.service.IWritingDraftService;
import com.personblog.api.AIAPI.AiArticleDraftApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 写作草稿服务实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WritingDraftServiceImpl implements IWritingDraftService, AiArticleDraftApi {

    private final WritingDraftMapper writingDraftMapper;

    @Override
    public WritingDraft getByTaskId(Long taskId) {
        LambdaQueryWrapper<WritingDraft> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WritingDraft::getTaskId, taskId);
        return writingDraftMapper.selectOne(queryWrapper);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public WritingDraft saveDraft(Long taskId, Long userId, Map<String, Object> draftData) {
        // 先检查是否已存在草稿，存在则更新
        WritingDraft existingDraft = getByTaskId(taskId);
        
        if (existingDraft != null) {
            // 更新现有草稿
            updateDraftFromData(existingDraft, draftData);
            writingDraftMapper.updateById(existingDraft);
            log.info("[WritingDraft] 更新草稿成功, draftId={}, taskId={}", existingDraft.getId(), taskId);
            return existingDraft;
        }

        // 创建新草稿
        WritingDraft draft = new WritingDraft();
        draft.setTaskId(taskId);
        draft.setUserId(userId);
        updateDraftFromData(draft, draftData);
        draft.setCreatedAt(LocalDateTime.now());
        writingDraftMapper.insert(draft);
        log.info("[WritingDraft] 保存草稿成功, draftId={}, taskId={}", draft.getId(), taskId);
        return draft;
    }

    /**
     * 从数据Map更新草稿实体
     */
    private void updateDraftFromData(WritingDraft draft, Map<String, Object> draftData) {
        if (draftData.get("title") != null) {
            draft.setTitle((String) draftData.get("title"));
        }
        if (draftData.get("summary") != null) {
            draft.setSummary((String) draftData.get("summary"));
        }
        if (draftData.get("content") != null) {
            draft.setContent((String) draftData.get("content"));
        }
        if (draftData.get("cover") != null) {
            draft.setCover((String) draftData.get("cover"));
        }
        if (draftData.get("category_id") != null) {
            draft.setCategoryId(Long.parseLong(draftData.get("category_id").toString()));
        }
        if (draftData.get("category_name") != null) {
            draft.setCategoryName((String) draftData.get("category_name"));
        }
        if (draftData.get("tag_ids") instanceof List) {
            draft.setTagIds(toCommaSeparated((List<?>) draftData.get("tag_ids")));
        }
        if (draftData.get("tag_names") instanceof List) {
            draft.setTagNames(toCommaSeparated((List<?>) draftData.get("tag_names")));
        }
    }

    /**
     * 将列表转为逗号分隔字符串
     */
    private String toCommaSeparated(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(list.get(i).toString());
        }
        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByTaskId(Long taskId) {
        LambdaQueryWrapper<WritingDraft> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WritingDraft::getTaskId, taskId);
        int rows = writingDraftMapper.delete(queryWrapper);
        if (rows > 0) {
            log.info("[WritingDraft] 删除草稿成功, taskId={}", taskId);
        }
    }
}
