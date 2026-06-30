package com.personblog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.ai.entity.WritingReflection;
import com.personblog.ai.mapper.WritingReflectionMapper;
import com.personblog.ai.service.IWritingReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 写作反思评价服务实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WritingReflectionServiceImpl implements IWritingReflectionService {

    private final WritingReflectionMapper writingReflectionMapper;

    @Override
    public WritingReflection getById(Long id) {
        return writingReflectionMapper.selectById(id);
    }

    @Override
    public WritingReflection getByTaskId(Long taskId) {
        LambdaQueryWrapper<WritingReflection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WritingReflection::getTaskId, taskId);
        return writingReflectionMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public WritingReflection saveReflection(Long taskId, Map<String, Object> reflectionData) {
        WritingReflection reflection = new WritingReflection();
        reflection.setTaskId(taskId);

        // 设置综合评分
        if (reflectionData.get("score") != null) {
            reflection.setScore(new BigDecimal(reflectionData.get("score").toString()));
        }

        // 设置各维度评分
        if (reflectionData.get("completeness") != null) {
            reflection.setCompleteness(new BigDecimal(reflectionData.get("completeness").toString()));
        }
        if (reflectionData.get("structure") != null) {
            reflection.setStructure(new BigDecimal(reflectionData.get("structure").toString()));
        }
        if (reflectionData.get("expression") != null) {
            reflection.setExpression(new BigDecimal(reflectionData.get("expression").toString()));
        }
        if (reflectionData.get("practicality") != null) {
            reflection.setPracticality(new BigDecimal(reflectionData.get("practicality").toString()));
        }
        if (reflectionData.get("format") != null) {
            reflection.setFormat(new BigDecimal(reflectionData.get("format").toString()));
        }

        // 设置优点、不足、建议（转为JSON字符串）
        if (reflectionData.get("strengths") instanceof List) {
            reflection.setStrengths(toJsonString((List<String>) reflectionData.get("strengths")));
        }
        if (reflectionData.get("weaknesses") instanceof List) {
            reflection.setWeaknesses(toJsonString((List<String>) reflectionData.get("weaknesses")));
        }
        if (reflectionData.get("suggestions") instanceof List) {
            reflection.setSuggestions(toJsonString((List<String>) reflectionData.get("suggestions")));
        }

        reflection.setCreatedAt(LocalDateTime.now());
        writingReflectionMapper.insert(reflection);
        log.info("[WritingReflection] 保存反思评价成功, reflectionId={}, taskId={}", reflection.getId(), taskId);
        return reflection;
    }

    /**
     * 将列表转为JSON字符串
     */
    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
