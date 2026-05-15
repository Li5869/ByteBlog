package com.personblog.ai.constants;

/**
 * AI 提示词模板常量类
 * <p>
 * 存放所有 AI 业务场景中使用的提示词文本，统一管理避免硬编码。
 *
 * @author LSH
 */
public final class AiPromptConstants {

    private AiPromptConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /** 文章润色相关提示词 */
    public static final class Polish {
        /** 润色请求的开头引导语 */
        public static final String PREFIX = "请对以下文章进行润色优化，风格要求：";
        /** 友好亲切风格描述 */
        public static final String STYLE_FRIENDLY = "友好亲切，通俗易懂。\n\n";
        /** 简洁精炼风格描述 */
        public static final String STYLE_CONCISE = "简洁精炼，直击要点。\n\n";
        /** 专业严谨风格描述（默认风格） */
        public static final String STYLE_PROFESSIONAL = "专业严谨，表达准确。\n\n";
        /** 文章标题前置标签 */
        public static final String TITLE_LABEL = "文章标题：";
        /** 文章内容前置标签 */
        public static final String CONTENT_LABEL = "文章内容：\n";
        /** 结束指令：要求直接输出润色后内容，无需多余解释 */
        public static final String OUTPUT_INSTRUCTION = "请直接输出润色后的文章内容，不需要其他解释。";
        /** 润色完成后的备注提示 */
        public static final String POLISH_NOTE = "文章已润色完成";

        private Polish() {}
    }

    /** 文章摘要生成相关提示词 */
    public static final class Summary {
        /** 摘要请求开头引导 */
        public static final String PREFIX = "请为以下文章生成一段简洁的摘要，控制在";
        /** 摘要请求结尾（包含字数限制和换行） */
        public static final String SUFFIX = "字以内。\n\n";
        /** 文章标题前置标签 */
        public static final String TITLE_LABEL = "文章标题：";
        /** 文章内容前置标签 */
        public static final String CONTENT_LABEL = "文章内容：\n";
        /** 结束指令：要求直接输出摘要内容 */
        public static final String OUTPUT_INSTRUCTION = "请直接输出摘要内容，不需要其他解释。";
        /** AI 提示词中的默认最大摘要字数 */
        public static final int DEFAULT_MAX_LENGTH = 100;

        private Summary() {}
    }

    /** 内容审核相关常量 */
    public static final class Moderation {
        /** 评论类型描述 */
        public static final String TYPE_COMMENT = "评论";
        /** 问题类型描述（问答模块） */
        public static final String TYPE_QUESTION = "问题";
        /** 文章类型描述（默认） */
        public static final String TYPE_ARTICLE = "文章";
        /** 默认标题（当审核内容无标题时使用） */
        public static final String DEFAULT_TITLE = "内容";
        /** SSE 通知事件中的审核类型标识 */
        public static final String SSE_TYPE = "moderation";
        /** 审核通过的动作类型 */
        public static final String ACTION_APPROVED = "moderation_approved";
        /** 审核拒绝的动作类型 */
        public static final String ACTION_REJECTED = "moderation_rejected";

        private Moderation() {}
    }

    /** 文章标题生成相关常量 */
    public static final class TitleGeneration {
        /** AI 提示词中的默认最大标题字数 */
        public static final int DEFAULT_MAX_LENGTH = 30;
        /** 文章内容截断的最大字符数 */
        public static final int MAX_CONTENT_LENGTH = 3000;
        /** 内容截断后追加的后缀 */
        public static final String TRUNCATION_SUFFIX = "...";
        /** 清洗标题时去除序号前缀的正则表达式（匹配 "1."、"1、"、"1)"、"1）" 等形式） */
        public static final String TITLE_NUMBER_REGEX = "^[0-9]+[.、)）]\\s*";

        private TitleGeneration() {}
    }
}
