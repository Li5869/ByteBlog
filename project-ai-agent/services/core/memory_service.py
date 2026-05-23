"""
Redis 记忆服务
文件：project-ai-agent/services/core/memory_service.py
"""

import redis.asyncio as redis
import json
from typing import List, Optional
from datetime import datetime
from pydantic import BaseModel, Field
from loguru import logger

from config.settings import get_settings
from common.constants import REDIS_KEY_CHAT_PREFIX, DEFAULT_TTL_SECONDS


class ToolCall(BaseModel):
    """工具调用信息"""
    id: int = Field(..., description="工具调用ID")
    name: str = Field(..., description="工具名称")
    args: dict = Field(default_factory=dict, description="工具参数")
    result: Optional[str] = Field(default=None, description="工具执行结果")


class MessageItem(BaseModel):
    """消息项"""
    role: str
    content: Optional[str] = None
    thinking: Optional[str] = Field(default=None, description="思考过程（仅用于展示，不参与上下文拼接）")
    tool_calls: Optional[List[ToolCall]] = Field(default=None, description="工具调用列表")
    created_at: datetime = Field(default_factory=datetime.now)


class RedisMemoryService:
    """Redis 记忆服务（异步）"""

    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return

        settings = get_settings()

        self._redis_url = settings.redis_url
        self._redis_client: Optional[redis.Redis] = None

        self.ttl = DEFAULT_TTL_SECONDS
        self._initialized = True
        logger.info("Redis 记忆服务初始化完成（异步模式）")

    async def _get_client(self) -> redis.Redis:
        """获取异步 Redis 客户端"""
        if self._redis_client is None:
            self._redis_client = redis.from_url(
                self._redis_url,
                decode_responses=True
            )
        return self._redis_client

    def _get_key(self, conversation_id: str) -> str:
        """生成 Redis key"""
        return f"{REDIS_KEY_CHAT_PREFIX}{conversation_id}"

    async def add_message(
        self,
        conversation_id: str,
        role: str,
        content: Optional[str] = None,
        thinking: Optional[str] = None,
        tool_calls: Optional[List[ToolCall]] = None
    ):
        """
        添加消息到记忆

        Args:
            conversation_id: 会话 ID
            role: 角色（user/assistant）
            content: 消息内容
            thinking: 思考过程（仅用于展示，不参与上下文拼接）
            tool_calls: 工具调用列表（assistant 消息）
        """
        client = await self._get_client()
        key = self._get_key(conversation_id)

        message = MessageItem(
            role=role,
            content=content,
            thinking=thinking,
            tool_calls=tool_calls,
            created_at=datetime.now()
        )

        await client.rpush(key, message.model_dump_json())
        await client.expire(key, self.ttl)

    async def get_messages(
        self,
        conversation_id: str,
        limit: int = 20
    ) -> List[MessageItem]:
        """
        获取会话历史消息

        Args:
            conversation_id: 会话 ID
            limit: 最大消息数量

        Returns:
            消息列表
        """
        client = await self._get_client()
        key = self._get_key(conversation_id)

        messages_json = await client.lrange(key, -limit, -1)

        messages = []
        for msg_json in messages_json:
            data = json.loads(msg_json)
            messages.append(MessageItem(**data))

        return messages

    async def get_context_string(
        self,
        conversation_id: str,
        limit: int = 10,
        include_thinking: bool = False
    ) -> str:
        """
        获取上下文字符串

        用于拼接历史消息作为上下文。
        
        Args:
            conversation_id: 会话 ID
            limit: 最大消息数量
            include_thinking: 是否包含思考内容（默认 False，符合 DeepSeek 官方建议）
        
        Returns:
            上下文字符串
        """
        messages = await self.get_messages(conversation_id, limit)

        context_parts = []
        for msg in messages:
            if msg.role == "user":
                context_parts.append(f"用户: {msg.content}")
            elif msg.role == "assistant":
                if include_thinking and msg.thinking:
                    context_parts.append(f"AI思考: {msg.thinking}")
                
                if msg.tool_calls:
                    for tc in msg.tool_calls:
                        args_str = json.dumps(tc.args, ensure_ascii=False)
                        context_parts.append(f"AI: [调用工具: {tc.name}({args_str})]")
                        if tc.result:
                            context_parts.append(f"工具结果: {tc.result}")
                    
                    if msg.content:
                        context_parts.append(f"AI: {msg.content}")
                else:
                    context_parts.append(f"AI: {msg.content}")

        return "\n".join(context_parts)

    async def clear_memory(self, conversation_id: str):
        """清空会话记忆"""
        client = await self._get_client()
        key = self._get_key(conversation_id)
        await client.delete(key)

    async def close(self):
        """关闭 Redis 连接"""
        if self._redis_client:
            await self._redis_client.close()
            self._redis_client = None


_memory_service: Optional[RedisMemoryService] = None


def get_memory_service() -> RedisMemoryService:
    """获取记忆服务单例"""
    global _memory_service
    if _memory_service is None:
        _memory_service = RedisMemoryService()
    return _memory_service
