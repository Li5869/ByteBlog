package com.personblog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.ai.entity.WritingDraft;
import com.personblog.ai.entity.WritingReflection;
import com.personblog.ai.mapper.WritingDraftMapper;
import com.personblog.ai.service.IWritingDraftService;
import com.personblog.ai.service.IWritingReflectionService;
import com.personblog.ai.vo.WritingDraftVO;
import com.personblog.api.AIAPI.AiArticleDraftApi;
import com.personblog.api.adminAPI.TagApi;
import com.personblog.api.adminAPI.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
    private final IWritingReflectionService writingReflectionService;
    private final TagApi tagApi;
    @Override
    public WritingDraftVO getByTaskId(Long taskId) {
        // 查询草稿
        LambdaQueryWrapper<WritingDraft> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WritingDraft::getTaskId, taskId);
        WritingDraft draft = writingDraftMapper.selectOne(queryWrapper);

        if (draft == null) {
            return null;
        }

        // 查询评估结果
        WritingReflection reflection = writingReflectionService.getByTaskId(taskId);

        // 组装完整的 VO
        // 评估结果字段
        return WritingDraftVO.builder()
                .title(draft.getTitle())
                .summary(draft.getSummary())
                .content(draft.getContent())
                .cover(draft.getCover())
                .categoryName(draft.getCategoryName())
                .categoryId(draft.getCategoryId())
                .tagIds(draft.getTagIds())
                .tagNames(draft.getTagNames())
                .allTagNames(parseTagNames(draft.getTagNames(),draft.getTagIds()))
                .createdAt(draft.getCreatedAt())
                // 评估结果字段
                .score(reflection != null ? reflection.getScore() : null)
                .completeness(reflection != null ? reflection.getCompleteness() : null)
                .structure(reflection != null ? reflection.getStructure() : null)
                .expression(reflection != null ? reflection.getExpression() : null)
                .practicality(reflection != null ? reflection.getPracticality() : null)
                .format(reflection != null ? reflection.getFormat() : null)
                .strengths(reflection != null ? reflection.getStrengths() : null)
                .weaknesses(reflection != null ? reflection.getWeaknesses() : null)
                .suggestions(reflection != null ? reflection.getSuggestions() : null)
                .build();
    }

    /**
     * 解析逗号分隔的标签名称为列表
     */
    private List<String> parseTagNames(String tagNames,String tagIds) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> res = new ArrayList<>(Arrays.stream(tagNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        List<Long> ids = Arrays.stream(tagIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        List<String> tags = tagApi.getTagsByIds(ids).stream().map(TagVO::getName).toList();
        res.addAll(tags);
        return res;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WritingDraft saveDraft(Long taskId, Long userId, Map<String, Object> draftData) {
        // 先检查是否已存在草稿，存在则更新
        WritingDraft existingDraft = getDraftEntityByTaskId(taskId);

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
     * 根据任务ID获取草稿实体（内部使用）
     */
    private WritingDraft getDraftEntityByTaskId(Long taskId) {
        LambdaQueryWrapper<WritingDraft> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WritingDraft::getTaskId, taskId);
        return writingDraftMapper.selectOne(queryWrapper);
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
