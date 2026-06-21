"""
用户长期记忆服务
文件：project-ai-agent/services/core/long_term_memory_service.py

封装 Mem0 引擎，提供跨会话的用户记忆能力。
使用 AsyncMemory 原生异步接口，无需 asyncio.to_thread 包装。
"""

from typing import Optional, List, Dict, Any
from loguru import logger

from config.settings import get_settings
from common.constants import MEMORY_TYPE_SEMANTIC, MEMORY_TYPE_EPISODIC, MEMORY_TYPE_PROCEDURAL


class LongTermMemoryService:
    """
    用户长期记忆服务（封装 Mem0 引擎）

    提供记忆的增删改查能力，支持三类记忆：
    - 语义记忆（semantic）：用户的偏好、属性、知识背景
    - 情节记忆（episodic）：关键对话的摘要
    - 程序记忆（procedural）：用户的交互习惯
    """

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
        self._enabled = settings.memory_enabled
        self._top_k = settings.memory_recall_top_k
        self._memory = None

        if self._enabled:
            try:
                from mem0 import AsyncMemory
                self._memory = AsyncMemory.from_config(settings.memory_config)
                logger.info("长期记忆服务初始化成功（AsyncMemory）")
            except Exception as e:
                logger.error(f"长期记忆服务初始化失败: {e}")
                self._enabled = False
        else:
            logger.info("长期记忆服务已禁用")

        self._initialized = True

    async def add_memory(
        self,
        messages: List[Dict[str, str]],
        user_id: str,
        metadata: Optional[Dict[str, Any]] = None
    ) -> Optional[Dict[str, Any]]:
        """
        添加记忆

        Mem0 会自动从对话中提取关键信息并存储。

        Args:
            messages: 消息列表，格式 [{"role": "user", "content": "..."}, ...]
            user_id: 用户 ID
            metadata: 额外元数据

        Returns:
            添加的记忆信息，如果功能禁用则返回 None
        """
        if not self._enabled or not self._memory:
            logger.debug("长期记忆服务未启用，跳过 add_memory")
            return None

        try:
            result = await self._memory.add(
                messages=messages,
                user_id=user_id,
                metadata=metadata or {}
            )
            logger.info(f"成功添加记忆: user_id={user_id}, result={result}")
            return result
        except Exception as e:
            logger.error(f"添加记忆失败: {e}")
            return None

    async def recall_memories(
        self,
        user_id: str,
        query: str,
        top_k: Optional[int] = None
    ) -> Dict[str, List[Dict[str, Any]]]:
        """
        召回相关记忆（首轮对话时调用）

        使用 Mem0 多信号混合检索：语义 + 关键词 + 实体，自动按相关性排序。

        Args:
            user_id: 用户 ID
            query: 查询内容（通常是用户当前消息）
            top_k: 返回数量，默认使用配置值

        Returns:
            按类型分组的记忆字典：
            {
                "profile": [...],    # 语义记忆
                "habits": [...],     # 程序记忆
                "episodes": [...]    # 情节记忆
            }
        """
        if not self._enabled or not self._memory:
            logger.debug("长期记忆服务未启用，跳过 recall_memories")
            return {"profile": [], "habits": [], "episodes": []}

        try:
            k = top_k or self._top_k
            results = await self._memory.search(
                query=query,
                filters={"user_id": user_id},
                limit=k
            )

            # 按记忆类型分组
            profile = []  # 语义记忆
            habits = []   # 程序记忆
            episodes = []  # 情节记忆

            memories = results.get("results", []) if isinstance(results, dict) else results

            for mem in memories:
                mem_dict = mem if isinstance(mem, dict) else mem.__dict__
                categories = mem_dict.get("categories", [])
                memory_type = mem_dict.get("metadata", {}).get("memory_type", "")

                # 根据 categories 或 metadata 判断类型
                if MEMORY_TYPE_SEMANTIC in categories or memory_type == MEMORY_TYPE_SEMANTIC:
                    profile.append(mem_dict)
                elif MEMORY_TYPE_PROCEDURAL in categories or memory_type == MEMORY_TYPE_PROCEDURAL:
                    habits.append(mem_dict)
                elif MEMORY_TYPE_EPISODIC in categories or memory_type == MEMORY_TYPE_EPISODIC:
                    episodes.append(mem_dict)
                else:
                    # 默认归类为语义记忆
                    profile.append(mem_dict)

            logger.info(
                f"召回记忆: user_id={user_id}, "
                f"profile={len(profile)}, habits={len(habits)}, episodes={len(episodes)}"
            )
            return {"profile": profile, "habits": habits, "episodes": episodes}

        except Exception as e:
            logger.error(f"召回记忆失败: {e}")
            return {"profile": [], "habits": [], "episodes": []}

    async def get_all_memories(self, user_id: str) -> List[Dict[str, Any]]:
        """
        获取用户所有记忆

        Args:
            user_id: 用户 ID

        Returns:
            记忆列表
        """
        if not self._enabled or not self._memory:
            logger.debug("长期记忆服务未启用，跳过 get_all_memories")
            return []

        try:
            result = await self._memory.get_all(filters={"user_id": user_id})
            memories = result.get("results", []) if isinstance(result, dict) else result
            logger.info(f"获取用户所有记忆: user_id={user_id}, count={len(memories)}")
            return memories
        except Exception as e:
            logger.error(f"获取用户记忆失败: {e}")
            return []

    async def get_memory(self, memory_id: str) -> Optional[Dict[str, Any]]:
        """
        获取指定记忆

        Args:
            memory_id: 记忆 ID

        Returns:
            记忆信息
        """
        if not self._enabled or not self._memory:
            logger.debug("长期记忆服务未启用，跳过 get_memory")
            return None

        try:
            result = await self._memory.get(memory_id=memory_id)
            return result
        except Exception as e:
            logger.error(f"获取记忆失败: memory_id={memory_id}, error={e}")
            return None

    async def update_memory(self, memory_id: str, data: str) -> Optional[Dict[str, Any]]:
        """
        更新记忆

        Args:
            memory_id: 记忆 ID
            data: 新的记忆内容

        Returns:
            更新后的记忆信息
        """
        if not self._enabled or not self._memory:
            logger.debug("长期记忆服务未启用，跳过 update_memory")
            return None

        try:
            result = await self._memory.update(memory_id=memory_id, data=data)
            logger.info(f"更新记忆成功: memory_id={memory_id}")
            return result
        except Exception as e:
            logger.error(f"更新记忆失败: memory_id={memory_id}, error={e}")
            return None

    async def delete_memory(self, memory_id: str) -> bool:
        """
        删除指定记忆

        Args:
            memory_id: 记忆 ID

        Returns:
            是否删除成功
        """
        if not self._enabled or not self._memory:
            logger.debug("长期记忆服务未启用，跳过 delete_memory")
            return False

        try:
            await self._memory.delete(memory_id=memory_id)
            logger.info(f"删除记忆成功: memory_id={memory_id}")
            return True
        except Exception as e:
            logger.error(f"删除记忆失败: memory_id={memory_id}, error={e}")
            return False

    async def clear_user_memories(self, user_id: str) -> bool:
        """
        清空用户所有记忆

        Args:
            user_id: 用户 ID

        Returns:
            是否清空成功
        """
        if not self._enabled or not self._memory:
            logger.debug("长期记忆服务未启用，跳过 clear_user_memories")
            return False

        try:
            await self._memory.delete_all(user_id=user_id)
            logger.info(f"清空用户记忆成功: user_id={user_id}")
            return True
        except Exception as e:
            logger.error(f"清空用户记忆失败: user_id={user_id}, error={e}")
            return False

    async def get_memory_history(self, memory_id: str) -> List[Dict[str, Any]]:
        """
        获取记忆变更历史

        Args:
            memory_id: 记忆 ID

        Returns:
            变更历史列表
        """
        if not self._enabled or not self._memory:
            logger.debug("长期记忆服务未启用，跳过 get_memory_history")
            return []

        try:
            result = await self._memory.history(memory_id=memory_id)
            return result if isinstance(result, list) else []
        except Exception as e:
            logger.error(f"获取记忆历史失败: memory_id={memory_id}, error={e}")
            return []


_long_term_memory_service: Optional[LongTermMemoryService] = None


def get_long_term_memory_service() -> LongTermMemoryService:
    """获取长期记忆服务单例"""
    global _long_term_memory_service
    if _long_term_memory_service is None:
        _long_term_memory_service = LongTermMemoryService()
    return _long_term_memory_service
