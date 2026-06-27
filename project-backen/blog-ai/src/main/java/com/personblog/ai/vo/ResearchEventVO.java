package com.personblog.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 研究事件VO（SSE 传输对象）
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchEventVO {

    /**
     * 事件类型：
     * - thinking: 思维链
     * - clarification: 澄清问题
     * - plan_approval: 计划确认
     * - phase: 任务执行进度
     * - stage_insight: 阶段洞察
     * - replan: 重规划通知
     * - report_ready: 报告完成
     * - error: 错误
     * - done: 任务结束
     */
    private String type;

    /**
     * 事件数据（具体结构取决于事件类型）
     */
    private Object data;

    /**
     * 完成事件（单例）
     */
    public static final ResearchEventVO DONE_EVENT = ResearchEventVO.builder()
            .type("done")
            .data(null)
            .build();
}
