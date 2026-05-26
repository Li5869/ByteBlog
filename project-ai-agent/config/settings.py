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
    openai_base_url: str = "https://api.openai.com"
    openai_api_key: str = ""

    # 具体模型名称
    model_name: str = "gpt-4o-mini"
    embedding_model: str = "text-embedding-3-small"

    # DeepSeek 专属（写作 Agent 等高创意场景）
    model_name_deepseek: str = "deepseek-chat"
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
