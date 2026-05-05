# AI 智能对话 Agent — LangGraph 重设计文档

## 一、问题背景

### 原有方案的问题

原方案使用 `langchain-classic` 的 `create_tool_calling_agent` + `AgentExecutor`。
该方案将**思考过程**、**工具调用**和**最终回答**混合在同一个 LLM 流式输出中，
前端需要依赖格式化的 XML 标签来区分三种内容：

```xml
<thinking>思考过程</thinking>
<answer>最终回答</answer>
```

**缺陷：**
- 模型输出不稳定，时常遗漏标签、标签不闭合、内容外泄
- 需要大量正则和字符串解析代码做后处理（`_clean_tags`、`get_current_tag_state` 等）
- 维护成本高，每次模型升级都可能引入新问题
- 多轮工具调用时标签嵌套逻辑复杂，易出错

---

## 二、新架构设计

### 2.1 核心思路

使用 **LangGraph StateGraph** 将"思考"和"回答"拆分为两个独立的 LLM 调用节点，
通过图的路由机制自然区分内容类型，**彻底消除 XML 标签依赖**。

### 2.2 工作流图

```
                    ┌──────────┐
                    │  think   │  ← LLM 绑定工具，流式输出思考
                    └────┬─────┘
                         │
              ┌──────────┼──────────┐
              │ 有工具调用 │ 无工具调用 │
              ↓          │          ↓
        ┌───────────┐   │    ┌──────────┐
        │execute_   │   │    │  answer  │  ← LLM 基于思考+工具结果生成回答
        │  tools    │   │    └──────────┘
        └─────┬─────┘   │         │
              │          │         ↓
              └──────────┘       END
```

### 2.3 节点说明

| 节点 | 角色 | LLM 配置 | 输出内容 |
|------|------|---------|---------|
| **think** | 思考+决策 | `bind_tools(ALL_TOOLS)` | 思考文本 → `reasoning` 事件 + 可选工具调用 |
| **execute_tools** | 工具执行 | 无（直接调用工具） | 工具结果 → 追加到消息列表 |
| **answer** | 最终回答 | 纯 LLM（不绑定工具） | 回答文本 → `token` 事件 |

### 2.4 状态定义

```python
class AgentState(TypedDict):
    user_input: str          # 原始用户输入
    messages: List[Any]      # 完整消息历史（LLM 上下文）
    thinking: str            # 累积的思考内容
    tool_results: List[str]  # 工具执行结果
    iteration: int           # 当前循环次数
    final_answer: str        # 最终回答
    deep_thinking: bool      # 是否深度思考
```

---

## 三、关键实现细节

### 3.1 流式事件机制

使用 `asyncio.Queue` 作为节点间事件通信通道：

```
think 节点           execute_tools 节点    answer 节点
    │                     │                   │
    ├─ put(reasoning) ────┤                   │
    ├─ put(tool_call) ────┤                   │
    │                     ├─ (执行工具) ─────┤
    │                     │                   ├─ put(token) ────
    │                     │                   ├─ put(token) ────
    │                     │                   └─ put(done) ─────
```

`astream_chat_with_result` 方法在后台执行图，同时从队列消费事件：

```python
async def astream_chat_with_result(self, message, deep_thinking=False):
    self._event_queue = asyncio.Queue()
    task = asyncio.create_task(run_graph())

    while True:
        event = await self._event_queue.get()
        if event.event_type == "done":
            yield event
            break
        yield event
```

### 3.2 think 节点的流式处理

关键代码模式——累积所有 chunk 以获得完整 `tool_calls`：

```python
async for chunk in llm_with_tools.astream(messages):
    full_response = chunk if full_response is None else full_response + chunk
    if chunk.content:
        # 实时推送推理内容
        await self._emit(StreamEvent(event_type="reasoning", content=chunk.content))

# 循环结束后，full_response 包含完整的 tool_calls
tool_calls = full_response.tool_calls if hasattr(full_response, "tool_calls") else []
```

使用 `full_response = full_response + chunk`（LangChain 内置合并逻辑），
无需手动解析 tool_call_chunks。

### 3.3 路由逻辑

```python
@staticmethod
def _route_after_think(state: AgentState) -> Literal["execute_tools", "answer"]:
    # 超过最大迭代次数 → 强制回答
    if state.get("iteration", 0) >= max_iter:
        return "answer"

    last_message = state["messages"][-1]
    has_tools = hasattr(last_message, "tool_calls") and bool(last_message.tool_calls)
    return "execute_tools" if has_tools else "answer"
```

### 3.4 answer 节点的提示词

```python
answer_prompt = f"""你已掌握以下信息，请直接回答用户问题。

## 你的思考过程
{thinking}

## 工具查询结果
{tool_results}

## 用户问题
{user_input}"""
```

answer 节点使用**不绑定工具**的纯 LLM，确保输出全是最终回答，不会产生工具调用。

---

## 四、事件类型协议

| event_type | 来源 | 含义 | 前端处理 |
|-----------|------|------|---------|
| `reasoning` | think 节点 | 思考过程片段 | `thinking += content` |
| `tool_call` | think 节点（检测到工具调用） | 调用工具通知 | 显示"🔧 调用工具: xxx" |
| `token` | answer 节点 | 最终回答片段 | `content += content` |
| `done` | run_graph 结束 | 流结束 | 停止接收，保存消息 |

### Java 端映射

| 原始 type | ChatEventTypeEnum | value |
|-----------|------------------|-------|
| `reasoning` | REASONING | 4 |
| `token` | DATA | 1 |
| `tool_call` | PARAM | 3 |
| `done` | STOP | 2 |

> Java 端代码无需修改，事件格式与之前完全一致。

---

## 五、与其他模块的协作

### 5.1 数据流

```
用户输入
    │
    ▼
chat_router.py          ← FastAPI SSE 端点
    │
    ▼
PythonAiChatService.java ← Spring Boot WebClient
    │
    ▼
smart_agent.py          ← LangGraph StateGraph
    ├─ think_node       ← LLM + tools (bind_tools)
    ├─ execute_tools    ← 工具执行
    └─ answer_node      ← 纯 LLM
    │
    ▼
StreamEvent              ← asyncio.Queue
    │
    ▼
SSE Response             ← text/event-stream
```

### 5.2 内存保存

`chat_router.py` 中的 `_save_partial` 不再需要清理 XML 标签，
直接保存 `full_response` 即可：

```python
# 修改前
content = _clean_tags(full_response)
if content:
    await memory_service.add_message(..., content=content)

# 修改后
if full_response:
    await memory_service.add_message(..., content=full_response)
```

---

## 六、文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `agents/smart_agent.py` | **重写** | 从 `AgentExecutor` 迁移到 `StateGraph`，约 360 行 |
| `config/prompts.py` | **简化** | 删除 XML 标签提示词，改为自然语言描述 |
| `api/chat_router.py` | **简化** | 删除 `_clean_tags`、`re` 导入，约 -30 行 |
| (可选) `common/constants.py` | 可删除 | `THINKING_TAGS` 不再被引用 |

---

## 七、回滚方案

如需回滚到原方案，恢复以下文件即可：

1. `agents/smart_agent.py` ← 使用 Git 恢复
2. `config/prompts.py` ← 恢复原深度思考提示词
3. `api/chat_router.py` ← 恢复 `_clean_tags` 函数

```bash
git checkout HEAD~1 -- project-ai-agent/agents/smart_agent.py
git checkout HEAD~1 -- project-ai-agent/config/prompts.py
git checkout HEAD~1 -- project-ai-agent/api/chat_router.py
```
