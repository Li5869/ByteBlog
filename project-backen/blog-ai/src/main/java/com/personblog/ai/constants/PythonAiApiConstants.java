package com.personblog.ai.constants;

public final class PythonAiApiConstants {

    private PythonAiApiConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /** AI对话相关API路径 */
    public static final class Chat {
        /** 流式对话接口 */
        public static final String STREAM = "/api/v1/chat/stream";

        private Chat() {}
    }

    /** AI写作相关API路径 */
    public static final class Writing {
        /** 启动写作任务接口 */
        public static final String START = "/api/v1/writing/start";
        /** 恢复写作任务接口（占位符 {taskId} 替换为任务ID） */
        public static final String RESUME = "/api/v1/writing/{taskId}/resume";
        /** 流式获取写作进度接口（占位符 {taskId} 替换为任务ID） */
        public static final String STREAM = "/api/v1/writing/{taskId}/stream";
        /** 查询任务状态接口（占位符 {taskId} 替换为任务ID） */
        public static final String STATUS = "/api/v1/writing/{taskId}/status";

        private Writing() {}
    }

    /** 知识库相关API路径 */
    public static final class Knowledge {
        /** 上传文件到知识库接口 */
        public static final String FILE_UPLOAD = "/api/v1/knowledge/file";

        private Knowledge() {}
    }

    /** Python 服务统一响应中的 JSON 字段名 */
    public static final class Fields {
        /** 响应码字段 */
        public static final String CODE = "code";
        /** 响应消息字段 */
        public static final String MSG = "msg";
        /** 响应数据字段 */
        public static final String DATA = "data";
        /** 事件类型字段 */
        public static final String TYPE = "type";
        /** 内容字段 */
        public static final String CONTENT = "content";
        /** 会话ID字段（Python 服务使用下划线命名） */
        public static final String CONVERSATION_ID = "conversation_id";
        /** 任务ID字段 */
        public static final String TASK_ID = "task_id";
        /** 任务状态字段 */
        public static final String STATUS = "status";
        /** 分块数量字段 */
        public static final String CHUNK_COUNT = "chunk_count";
        /** 文件名（知识库上传结果的返回值） */
        public static final String FILENAME = "filename";
        /** ID列表字段 */
        public static final String IDS = "ids";

        private Fields() {}
    }

    /** SSE 流式事件类型名称 */
    public static final class SseEvent {
        /** 模型思考分析过程 */
        public static final String THINKING = "thinking";
        /** 回答文本片段（打字机效果） */
        public static final String CHUNK = "chunk";
        /** ReAct 循环中的工具调用 */
        public static final String TOOL_CALL = "tool_call";
        /** 工具执行结果 */
        public static final String TOOL_RESULT = "tool_result";
        /** 流式响应完成 */
        public static final String DONE = "done";
        /** 流式响应异常 */
        public static final String ERROR = "error";
        /** 写作任务阶段事件 */
        public static final String PHASE = "phase";
        /** 写作大纲就绪事件 */
        public static final String PLAN_READY = "plan_ready";
        /** 写作 token（文本片段）事件 */
        public static final String TOKEN = "token";
        /** 写作反思结果事件 */
        public static final String REFLECTION_RESULT = "reflection_result";
        /** 写作定稿就绪事件 */
        public static final String FINALIZE_READY = "finalize_ready";

        private SseEvent() {}
    }

    /** 通用消息文本 */
    public static final class Msg {
        /** Python 服务返回成功标识 */
        public static final String SUCCESS = "success";
        /** 服务异常前缀（用于 SSE 事件数据构建，非异常抛出） */
        public static final String SERVICE_ERROR = "服务异常: ";
        /** 错误前缀（用于 SSE 事件数据构建，非异常抛出） */
        public static final String ERROR_PREFIX = "错误: ";

        private Msg() {}
    }
}
