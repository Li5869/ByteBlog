"""
配置管理
文件：project-ai-agent/config/settings.py
"""

import os
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """应用配置（统一管理，所有配置必须通过此类读取，禁止直接 os.getenv）"""

    # ===== 应用基础 =====
    app_name: str = "AI Agent Service"
    debug: bool = False

    # ===== 数据库 & 缓存 =====
    database_url: str = "postgresql://postgres:postgres@localhost:5432/person_blog"
    redis_url: str = "redis://localhost:6379/2"

    # ===== Elasticsearch =====
    es_host: str = "http://localhost:9200"

    # ===== 向量维度（Embedding）=====
    vector_dimension: int = 1536

    # ===== Skill 向量化 =====
    skill_vector_table: str = "skill_chunks"
    skill_chunk_min_results: int = 3
    skill_similarity_threshold: float = 0.7
    skill_auto_index_on_startup: bool = False

    # ===== 服务 & 端口 =====
    host: str = "0.0.0.0"
    port: int = 8000

    # ===== LLM 配置（统一）=====
    openai_base_url: str = "https://api.chatanywhere.tech/v1"
    openai_api_key: str = ""

    # 具体模型名称
    embedding_model: str = "text-embedding-3-small"

    # DeepSeek 专属（写作 Agent 等高创意场景）
    # deepseek-v4-flash: 高效通用，非默认思考模式
    # deepseek-v4-pro: 强推理旗舰，默认启用思考模式
    model_name_deepseek: str = "deepseek-v4-flash"
    openai_base_url_deepseek: str = "https://api.deepseek.com"
    openai_api_key_deepseek: str = ""

    # ===== Agent 行为控制 =====
    max_iterations: int = 20
    writing_max_revisions: int = 3
    writing_reflection_threshold: float = 7.0

    # ===== Writing Agent 专属 LLM Temperature 配置（按角色差异化） =====
    writing_planner_temperature: float = 0.1
    writing_writer_temperature: float = 0.6
    writing_critic_temperature: float = 0.1
    writing_classifier_temperature: float = 0.2

    # ===== 外部 API =====
    tavily_api_key: str = ""

    # ===== Firecrawl 网页爬取 =====
    firecrawl_enabled: bool = True
    firecrawl_api_key: str = ""

    # ===== Judge0 代码执行 =====
    code_execution_enabled: bool = True
    code_execution_timeout: int = 10  # 执行超时时间（秒）

    # ===== 长期记忆配置（Mem0）=====
    memory_enabled: bool = True  # 记忆功能总开关
    memory_recall_top_k: int = 5  # 记忆召回数量
    memory_activity_marker_ttl: int = 300  # Redis 活跃标记 TTL（秒），需大于 XXL-Job 扫描间隔

    # ===== 中期记忆配置（LangMem 短期摘要）=====
    # 基于 token 阈值压缩对话历史：旧消息自动摘要，近期消息保持原样
    # DeepSeek 上下文窗口 1M，阈值设为 200K 触发摘要，留足空间给工具结果和响应
    mid_term_memory_enabled: bool = True  # 中期记忆功能总开关
    mid_term_memory_max_tokens: int = 150000  # 压缩后最终输出的 token 上限
    mid_term_memory_max_tokens_before_summary: int = 200000  # 触发摘要的 token 阈值
    mid_term_memory_max_summary_tokens: int = 2000  # 摘要本身的 token 预算

    @property
    def memory_config(self) -> dict:
        """Mem0 配置（自托管 pgvector）"""
        return {
            "vector_store": {
                "provider": "pgvector",
                "config": {
                    "connection_string": self.database_url,
                    "collection_name": "user_memories",
                    "embedding_model_dims": self.vector_dimension,
                }
            },
            "llm": {
                "provider": "openai",  # DeepSeek 兼容 OpenAI 接口
                "config": {
                    "model": self.model_name_deepseek,
                    "api_key": self.openai_api_key_deepseek,
                    "openai_base_url": self.openai_base_url_deepseek,
                }
            },
            "embedder": {
                "provider": "openai",
                "config": {
                    "model": self.embedding_model,
                    "api_key": self.openai_api_key,
                    "openai_base_url": self.openai_base_url,
                }
            }
        }

    # ===== 博客后端 API =====
    backend_api_base: str = "http://localhost:8080/api"
    backend_api_key: str = ""
    
    # ===== 前端域名（用于生成成果链接）=====
    frontend_base_url: str = "http://localhost:3000"

    # ===== Nacos 服务发现 =====
    nacos_enabled: bool = False
    nacos_server_addr: str = "127.0.0.1:8848"
    nacos_namespace: str = "public"
    nacos_group: str = "DEFAULT_GROUP"
    nacos_service_name: str = "python-ai"
    nacos_username: str = ""
    nacos_password: str = ""

    # ===== LangSmith 可观测性 =====
    langchain_tracing_v2: bool = False  # 是否开启追踪
    langchain_api_key: str = ""  # LangSmith API Key
    langchain_project: str = "byteblog-agent-dev"  # 项目名称
    langchain_endpoint: str = ""  # 可选：非美国区域需要设置（如 https://api.eu.langchain.com）

    # Pydantic v2 配置
    model_config = SettingsConfigDict(
        env_file=os.path.join(os.path.dirname(__file__), "..", ".env"),
        env_file_encoding="utf-8",
        extra="ignore"
    )


@lru_cache
def get_settings() -> Settings:
    """获取配置单例"""
    return Settings()
