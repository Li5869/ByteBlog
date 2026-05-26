package com.personblog.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.admin.aspect.RecordLog;
import com.personblog.ai.BizService.SkillService;
import com.personblog.ai.vo.SkillItemVO;
import com.personblog.ai.vo.SkillStatsVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 技能管理 Controller
 * RESTful 接口设计：
 * - POST /ai/skill/rebuild  重建索引
 * - GET  /ai/skill          分页查询 Skill 列表
 * - GET  /ai/skill/stats    获取统计信息
 *
 * @author LSH
 */
@Tag(name = "技能管理", description = "Skill 索引重建和状态查询")
@Slf4j
@RestController
@RequestMapping("/ai/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "重建 Skill 索引", description = "从 skills/ 目录读取所有 SKILL.md，切片后向量化入库，先清空旧数据再全量写入")
    @PostMapping("/rebuild")
    @RecordLog(Type = "create", businessType = "skill", description = "重建技能索引")
    public JsonData<SkillStatsVO> rebuildIndex() {
        SkillStatsVO result = skillService.rebuildIndex();
        return JsonData.buildSuccess(result);
    }

    @Operation(summary = "分页查询 Skill 列表", description = "返回已索引的 Skill 列表，支持分页")
    @GetMapping
    public JsonData<Page<SkillItemVO>> getSkillList(
            @Parameter(description = "当前页，默认 1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小，默认 10") @RequestParam(defaultValue = "10") Integer size
    ) {
        Page<SkillItemVO> result = skillService.getSkillList(current, size);
        return JsonData.buildSuccess(result);
    }

    @Operation(summary = "获取 Skill 统计信息", description = "返回 Skill 总数和切片总数")
    @GetMapping("/stats")
    public JsonData<SkillStatsVO> getStats() {
        SkillStatsVO result = skillService.getStats();
        return JsonData.buildSuccess(result);
    }
}