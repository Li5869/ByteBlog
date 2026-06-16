"""
Judge0 代码执行服务
文件：project-ai-agent/mcp_service/judge0_service.py

封装 Judge0 CE 公开 API，提供代码执行功能。
支持 60+ 编程语言，沙箱隔离执行。
"""

import time
from typing import Optional
from functools import lru_cache

import httpx
from loguru import logger


@lru_cache
def get_judge0_service() -> "Judge0Service":
    """获取 Judge0 服务单例（@lru_cache 保证单例）"""
    return Judge0Service()


# 常用语言 ID 映射
LANGUAGE_MAP = {
    "python": 71,           # Python 3.8
    "python3": 71,
    "javascript": 63,       # Node.js 12.14
    "node": 63,
    "typescript": 74,       # TypeScript 3.7
    "java": 62,             # Java OpenJDK 13
    "c": 50,                # GCC 9.2
    "cpp": 54,              # GCC 9.2
    "c++": 54,
    "go": 60,               # Go 1.13
    "rust": 73,             # Rust 1.40
    "ruby": 72,             # Ruby 2.7
    "php": 68,              # PHP 7.4
    "swift": 83,            # Swift 5.1
    "kotlin": 78,           # Kotlin 1.3
    "csharp": 51,           # C# Mono 6
    "c#": 51,
    "scala": 81,            # Scala 2.13
    "r": 80,                # R 4.0
    "lua": 64,              # Lua 5.3
    "perl": 85,             # Perl 5.30
    "haskell": 61,          # GHC 8.8
    "bash": 46,             # Bash 5.0
    "shell": 46,
    "sql": 82,              # SQLite 3.27
}


class Judge0Service:
    """
    Judge0 代码执行服务

    封装 Judge0 CE 公开 API，提供统一的代码执行接口。
    使用公开实例 https://ce.judge0.com/，无需 API Key。
    """

    BASE_URL = "https://ce.judge0.com"

    def __init__(self, timeout: int = 10):
        self.timeout = timeout
        self._client: Optional[httpx.AsyncClient] = None

    def _get_client(self) -> httpx.AsyncClient:
        """获取或创建 HTTP 客户端"""
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(
                timeout=httpx.Timeout(self.timeout + 5),
                headers={"Content-Type": "application/json"},
            )
        return self._client

    def _get_language_id(self, language: str) -> int:
        """获取语言 ID，支持语言名称和 ID"""
        # 如果是数字，直接返回
        if language.isdigit():
            return int(language)

        # 从映射中查找
        lang_id = LANGUAGE_MAP.get(language.lower())
        if lang_id is None:
            raise ValueError(f"不支持的语言: {language}。支持的语言: {', '.join(LANGUAGE_MAP.keys())}")
        return lang_id

    async def execute(
        self,
        code: str,
        language: str = "python",
        stdin: str = "",
        cpu_time_limit: int = 5,
        memory_limit: int = 128000,
    ) -> dict:
        """
        执行代码并返回结果

        Args:
            code: 要执行的源代码
            language: 编程语言（名称或 ID）
            stdin: 标准输入
            cpu_time_limit: CPU 时间限制（秒），默认 5
            memory_limit: 内存限制（KB），默认 128000（128MB）

        Returns:
            包含以下字段的字典：
            - success: 是否成功
            - stdout: 标准输出
            - stderr: 标准错误
            - compile_output: 编译输出
            - time: 执行时间（秒）
            - memory: 内存使用（KB）
            - status: 执行状态
            - language: 使用的语言
            - error: 错误信息（失败时）
        """
        try:
            lang_id = self._get_language_id(language)
        except ValueError as e:
            return {"success": False, "error": str(e)}

        client = self._get_client()

        # 构建请求体
        payload = {
            "source_code": code,
            "language_id": lang_id,
            "stdin": stdin,
            "cpu_time_limit": cpu_time_limit,
            "memory_limit": memory_limit,
        }

        try:
            logger.info(f"[Judge0] 执行代码: language={language}, length={len(code)}")
            start_time = time.time()

            # 同步执行（wait=true）
            response = await client.post(
                f"{self.BASE_URL}/submissions?base64_encoded=false&wait=true",
                json=payload,
            )
            response.raise_for_status()
            result = response.json()

            elapsed = time.time() - start_time
            logger.info(f"[Judge0] 执行完成: {elapsed:.2f}s")

            # 提取状态信息
            status = result.get("status", {})
            status_id = status.get("id", 0)
            status_desc = status.get("description", "Unknown")

            # 判断是否成功（状态 3 = Accepted, 4 = Wrong Answer 也算执行成功）
            is_success = status_id in [3, 4, 5, 6, 7, 8, 9, 10, 11, 12]

            return {
                "success": is_success,
                "stdout": result.get("stdout") or "",
                "stderr": result.get("stderr") or "",
                "compile_output": result.get("compile_output") or "",
                "time": result.get("time") or "0",
                "memory": result.get("memory") or 0,
                "status": status_desc,
                "status_id": status_id,
                "language": language,
            }

        except httpx.TimeoutException:
            logger.error(f"[Judge0] 执行超时: {self.timeout}s")
            return {
                "success": False,
                "error": f"代码执行超时（{self.timeout}秒）",
                "language": language,
            }
        except httpx.HTTPStatusError as e:
            logger.error(f"[Judge0] HTTP 错误: {e.response.status_code}")
            return {
                "success": False,
                "error": f"Judge0 API 错误: {e.response.status_code}",
                "language": language,
            }
        except Exception as e:
            logger.error(f"[Judge0] 执行失败: {e}")
            return {
                "success": False,
                "error": f"代码执行失败: {str(e)}",
                "language": language,
            }

    async def get_languages(self) -> list:
        """获取支持的语言列表"""
        client = self._get_client()
        try:
            response = await client.get(f"{self.BASE_URL}/languages")
            response.raise_for_status()
            return response.json()
        except Exception as e:
            logger.error(f"[Judge0] 获取语言列表失败: {e}")
            return []

    async def close(self):
        """关闭 HTTP 客户端"""
        if self._client and not self._client.is_closed:
            await self._client.aclose()
