"""
代码执行工具
文件：project-ai-agent/tools/code_execution_tool.py

提供代码执行的 LangChain Tool 定义，具体逻辑由 Judge0Service 实现。
支持 60+ 编程语言，沙箱隔离执行。
"""

from langchain_core.tools import tool

from mcp_service.judge0_service import get_judge0_service


@tool
async def execute_code(
    code: str,
    language: str = "python",
    stdin: str = "",
) -> dict:
    """
    执行代码并返回结果

    当用户要求执行、运行、验证代码时使用此工具。
    支持 60+ 编程语言，包括 Python、JavaScript、Java、C++、Go、Rust 等。

    Args:
        code: 要执行的源代码
        language: 编程语言，支持：python, javascript, typescript, java, c, cpp, go, rust, ruby, php, swift, kotlin, bash 等
        stdin: 标准输入（可选），程序需要读取输入时使用

    Returns:
        包含以下字段的字典：
        - success: 是否执行成功
        - stdout: 标准输出
        - stderr: 标准错误
        - compile_output: 编译输出（编译型语言）
        - time: 执行时间（秒）
        - memory: 内存使用（KB）
        - status: 执行状态描述
        - language: 使用的语言
        - error: 错误信息（如果失败）

    使用场景：
    1. 用户要求"运行这段代码"
    2. 用户问"这段代码输出什么"
    3. 用户要求"验证这个算法"
    4. 用户需要"测试这段代码"

    注意：
    - 代码在沙箱环境中执行，无法访问外部网络
    - 执行时间限制为 5 秒
    - 内存限制为 128MB
    """
    service = get_judge0_service()
    return await service.execute(code, language, stdin)


# 工具列表
CODE_EXECUTION_TOOLS = [execute_code]
