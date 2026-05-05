"""
FastAPI 应用入口
文件：project-ai-agent/main.py
"""

import sys

# ⚠️ Windows + psycopg v3 关键修复
# psycopg v3 异步模式底层依赖 select()，不兼容 Windows 默认的 ProactorEventLoop
# 必须在所有 asyncio 操作之前，将全局事件循环策略切换为 SelectorEventLoop
# 这样无论通过 python main.py 还是 uvicorn main:app 启动，都使用兼容的事件循环
if sys.platform == "win32":
    import asyncio
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from loguru import logger
from config.settings import get_settings
from api import chat_router, knowledge_router
from api.writing_router import router as writing_router


def setup_logging():
    """配置日志"""
    settings = get_settings()

    logger.remove()
    logger.add(
        sys.stdout,
        format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>",
        level="DEBUG" if settings.debug else "INFO"
    )
@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    settings = get_settings()
    setup_logging()

    logger.info(f"🚀 {settings.app_name} 启动中...")
    logger.info(f"🗄️ 数据库: {settings.database_url}")
    logger.info(f"📦 Redis: {settings.redis_url}")
    logger.info(f"🔍 ES: {settings.es_host}")

    yield

    logger.info("👋 应用关闭")


def create_app() -> FastAPI:
    """创建 FastAPI 应用"""
    settings = get_settings()

    app = FastAPI(
        title=settings.app_name,
        description="""
## AI Agent 服务

基于 LangChain + FastAPI 的 AI 问答服务。

### 功能模块

| 模块 | 说明 |
|------|------|
| Chat | 统一对话接口，支持多轮对话、上下文记忆、自动工具调用 |
| RAG | 知识问答，基于 pgvector |
| Knowledge | 知识库管理，文档上传与向量化 |

### 可用工具

对话接口会自动调用以下工具：
- **search_articles_by_keyword**: ES 文章搜索
- **search_knowledge_base**: pgvector 知识库搜索
- **get_hot_articles**: 获取热门文章
- **get_article_by_id**: 获取文章详情
        """,
        version="1.0.0",
        lifespan=lifespan
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(chat_router, prefix="/api/v1/chat", tags=["Chat"])
    app.include_router(knowledge_router, prefix="/api/v1/knowledge", tags=["Knowledge"])
    app.include_router(writing_router, prefix="/api/v1/writing", tags=["Writing"])

    @app.get("/health")
    async def health_check():
        return {"status": "ok", "service": settings.app_name}

    @app.get("/")
    async def root():
        return {
            "service": settings.app_name,
            "docs": "/docs",
            "health": "/health"
        }

    return app


app = create_app()

if __name__ == "__main__":
    import uvicorn
    settings = get_settings()
    # 全局 WindowsSelectorEventLoopPolicy 已在文件顶部设置，无需 loop_factory 特殊处理
    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        loop="asyncio",
        reload=settings.debug,
    )
