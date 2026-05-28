package com.personblog.ai.BizService;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.ai.vo.SkillItemVO;
import com.personblog.ai.vo.SkillStatsVO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.personblog.ai.constants.PythonAiApiConstants.*;
import static java.util.Map.Entry.comparingByKey;

/**
 * Skill 技能管理服务
 * 负责调用 Python AI 服务进行 Skill 索引重建和状态查询
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final WebClient pythonAiWebClient;

    /**
     * 重建 Skill 全量索引
     * 调用 Python POST /api/v1/skill/index
     */
    public SkillStatsVO rebuildIndex() {
        log.info("开始重建 Skill 索引");
        Map<String, Object> response = pythonAiWebClient.post()
                .uri(Skill.INDEX)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        Map<String, Object> data = parseResponse(response);
        log.info("Skill 索引重建完成: totalSkills={}, totalChunks={}",
                data.get("totalSkills"), data.get("totalChunks"));
        return toStatsVO(data);
    }

    /**
     * 分页查询 Skill 列表
     * 调用 Python GET /api/v1/skill/status，转换为 Page<SkillItemVO>
     *
     * @param current 当前页
     * @param size    每页大小
     */
    public Page<SkillItemVO> getSkillList(Integer current, Integer size) {
        Map<String, Object> response = pythonAiWebClient.get()
                .uri(Skill.STATUS)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        Map<String, Object> data = parseResponse(response);

        Map<String, Integer> skills = (Map<String, Integer>) data.get(Fields.SKILLS);
        if (skills == null || skills.isEmpty()) {
            Page<SkillItemVO> emptyPage = new Page<>(current != null ? current : 1, size != null ? size : 10);
            emptyPage.setRecords(List.of());
            emptyPage.setTotal(0);
            return emptyPage;
        }

        List<SkillItemVO> records = skills.entrySet().stream()
                .sorted(comparingByKey())
                .map(entry -> SkillItemVO.builder()
                        .name(entry.getKey())
                        .chunkCount(entry.getValue())
                        .status(entry.getValue() > 0 ? "indexed" : "empty")
                        .build())
                .collect(Collectors.toList());

        int pageNum = current != null && current > 0 ? current : 1;
        int pageSize = size != null && size > 0 ? size : 10;
        int total = records.size();

        Page<SkillItemVO> page = new Page<>(pageNum, pageSize, total);
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        page.setRecords(fromIndex < total ? records.subList(fromIndex, toIndex) : List.of());
        return page;
    }

    /**
     * 获取 Skill 统计信息
     * 调用 Python GET /api/v1/skill/status
     */
    public SkillStatsVO getStats() {
        Map<String, Object> response = pythonAiWebClient.get()
                .uri(Skill.STATUS)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        Map<String, Object> data = parseResponse(response);
        return toStatsVO(data);
    }

    /**
     * 解析 Python 服务的响应，提取 data 字段
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(Map<String, Object> response) {
        if (response == null || !Msg.SUCCESS.equals(response.get(Fields.MSG))) {
            String errorMsg = response != null ? (String) response.get(Fields.MSG) : "未知错误";
            log.error("Python Skill 服务调用失败: {}", errorMsg);
            throw new BizException(BizCodeEnum.AI_PYTHON_SERVICE_ERROR);
        }

        Map<String, Object> data = (Map<String, Object>) response.get(Fields.DATA);
        if (data == null) {
            throw new BizException(BizCodeEnum.AI_RESPONSE_EMPTY);
        }

        return data;
    }

    /**
     * 将 Python 响应转换为 SkillStatsVO
     * Python 返回字段名已统一为 camelCase，直接使用 BeanUtil 转换
     */
    private SkillStatsVO toStatsVO(Map<String, Object> data) {
        SkillStatsVO vo = BeanUtil.toBean(data, SkillStatsVO.class);
        if (vo.getTotalSkills() == null) vo.setTotalSkills(0);
        if (vo.getTotalChunks() == null) vo.setTotalChunks(0);
        return vo;
    }
}