package com.personblog.ai.constants;

import java.util.Set;

/**
 * AI 业务常量类
 * <p>
 * 存放 AI 业务逻辑中使用的样式、动作类型、默认值等常量。
 *
 * @author LSH
 */
public final class AiBusinessConstants {

    private AiBusinessConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /** 文章润色风格常量 */
    public static final class PolishStyle {
        /** 友好亲切风格 */
        public static final String FRIENDLY = "friendly";
        /** 简洁精炼风格 */
        public static final String CONCISE = "concise";

        private PolishStyle() {}
    }

    /** 写作任务操作类型常量 */
    public static final class WritingAction {
        /** 批准执行写作 */
        public static final String APPROVE = "approve";
        /** 修改大纲 */
        public static final String REVISE = "revise";

        private WritingAction() {}
    }

    /** 写作 SSE 事件中通用事件类型集合（除 done/error 外的事件类型） */
    public static final class WritingEventTypes {
        /** 通用事件类型集合：这些事件统一使用 type + data 结构构建 VO */
        public static final Set<String> GENERIC_TYPES = Set.of(
                PythonAiApiConstants.SseEvent.PHASE,
                PythonAiApiConstants.SseEvent.PLAN_READY,
                PythonAiApiConstants.SseEvent.TOKEN,
                PythonAiApiConstants.SseEvent.REFLECTION_RESULT,
                PythonAiApiConstants.SseEvent.FINALIZE_READY
        );

        private WritingEventTypes() {}
    }

    /** 通用默认值 */
    public static final class Defaults {
        /** 空字符串 */
        public static final String EMPTY = "";
        /** 默认内容长度（用于 content() null 保护时的兜底） */
        public static final String DEFAULT_CONTENT = "";

        private Defaults() {}
    }
}
