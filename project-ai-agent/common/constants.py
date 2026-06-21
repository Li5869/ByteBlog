"""
全局常量定义
"""

# ==================== Elasticsearch ====================
ES_INDEX_ARTICLE: str = "article"
ES_INDEX_AUTHOR: str = "author"
ES_REQUEST_TIMEOUT: int = 30
ES_DEFAULT_PAGE_SIZE: int = 10
ES_DEFAULT_SEARCH_SIZE: int = 5

# ==================== Redis Key 前缀 ====================
REDIS_KEY_CHAT_PREFIX: str = "chat:memory:"
REDIS_KEY_WRITING_TASK_PREFIX: str = "writing:task:"
REDIS_KEY_WRITING_EVENTS_PREFIX: str = "writing:events:"
REDIS_KEY_WRITING_PLAN_PREFIX: str = "writing:plan:"
REDIS_KEY_WRITING_REFERENCES_PREFIX: str = "writing:references:"

# ==================== 对话相关 ====================
THINKING_TAGS: tuple[str, ...] = (
    "<thinking>", "</thinking>", "<answer>", "</answer>",
    # 工具调用 XML 标签（DeepSeek 模型可能输出）
    "<invoke>", "</invoke>",
    "<tool_name>", "</tool_name>",
    "<tool_use>", "</tool_use>",
    "<parameter", "</parameter>",
    "<call>", "</call>",
)

# ==================== 默认过期时间 ====================
DEFAULT_TTL_SECONDS: int = 3600 * 24 * 7  # 7 天
WRITING_TASK_TTL_SECONDS: int = 7200      # 2 小时

# ==================== 长期记忆 ====================
MEMORY_TYPE_SEMANTIC: str = "semantic"    # 语义记忆：用户的偏好、属性、知识背景
MEMORY_TYPE_EPISODIC: str = "episodic"    # 情节记忆：关键对话的摘要
MEMORY_TYPE_PROCEDURAL: str = "procedural"  # 程序记忆：用户的交互习惯

# Redis 活跃标记 Key 前缀（用于 XXL-Job 调度记忆提取）
REDIS_KEY_CHAT_ACTIVE_PREFIX: str = "chat:active:"
