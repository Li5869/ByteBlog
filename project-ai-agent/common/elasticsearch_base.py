"""
Elasticsearch 服务基类

article 和 author 的 ES 操作高度相似，提取公共逻辑。
"""

from typing import List, Optional
from elasticsearch import AsyncElasticsearch
from loguru import logger

from config.settings import get_settings
from common.constants import ES_REQUEST_TIMEOUT


class ElasticsearchBaseService:
    """
    ES 服务抽象基类（类似 Java 的 abstract class）

    子类只需实现：
    - index_name: 索引名称
    - _map_hit(): 将 ES hit 映射为业务 dict
    """

    # 子类必须覆盖
    index_name: str = ""

    def __init__(self):
        settings = get_settings()
        self.client: AsyncElasticsearch = AsyncElasticsearch(
            settings.es_host,
            request_timeout=ES_REQUEST_TIMEOUT
        )
        logger.info(f"[ES] 服务初始化完成，索引: {self.index_name}")

    # ==================== 公共方法 ====================

    async def check_index_exists(self) -> bool:
        """检查索引是否存在"""
        try:
            return await self.client.indices.exists(index=self.index_name)
        except Exception as e:
            logger.error(f"[ES] 检查索引失败: {e}")
            return False

    async def search(
        self,
        query: dict,
        sort: Optional[list] = None,
        source: Optional[list] = None,
        size: int = 10,
    ) -> List[dict]:
        """
        通用搜索方法（类似 Java 的 search(Query)）

        Args:
            query: ES query DSL
            sort: 排序规则
            source: 返回字段列表
            size: 返回数量

        Returns:
            搜索结果列表
        """
        try:
            if not await self.check_index_exists():
                logger.warning(f"[ES] 索引 {self.index_name} 不存在，请先同步数据")
                return []

            body: dict = {"query": query, "size": size}
            if sort:
                body["sort"] = sort
            if source:
                body["_source"] = source

            response = await self.client.search(index=self.index_name, **body)
            return [self._map_hit(hit) for hit in response["hits"]["hits"]]

        except Exception as e:
            logger.error(f"[ES] 搜索失败: {e}")
            return []

    async def get_by_id(self, doc_id: str) -> Optional[dict]:
        """
        根据 ID 获取文档（类似 Java 的 getById()）

        Args:
            doc_id: 文档 ID

        Returns:
            文档数据，不存在返回 None
        """
        try:
            if not await self.check_index_exists():
                logger.warning(f"[ES] 索引 {self.index_name} 不存在")
                return None

            response = await self.client.get(index=self.index_name, id=doc_id)

            if response.get("found"):
                return self._map_source(response["_source"])

            return None
        except Exception as e:
            logger.error(f"[ES] 获取文档失败: {e}")
            return None

    async def close(self):
        """关闭连接（类似 Java 的 @PreDestroy）"""
        await self.client.close()

    # ==================== 子类必须实现 ====================

    def _map_hit(self, hit: dict) -> dict:
        """
        将 ES hit 映射为业务 dict（类似 Java 的 BeanUtils.copyProperties）

        子类必须实现此方法，定义字段映射规则。
        """
        raise NotImplementedError("子类必须实现 _map_hit() 方法")

    def _map_source(self, source: dict) -> dict:
        """
        将 ES _source 映射为业务 dict（用于 get_by_id）

        默认调用 _map_hit()，子类可覆盖
        """
        return self._map_hit({"_source": source, "_score": 0})
