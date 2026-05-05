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

# ==================== 数据库连接 ====================
PSYCOPG_SCHEME: str = "postgresql+psycopg://"
POSTGRES_SCHEME: str = "postgresql://"
