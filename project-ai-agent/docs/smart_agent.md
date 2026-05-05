# SmartAgent 技术实现文档

## 一、架构总览

### 1.1 职责定位

**SmartAgent** 是一个基于 **LangGraph StateGraph** 构建的**智能对话 Agent**，采用纯 **ReAct（Reasoning + Acting）** 范式。它的核心职责是：

- 理解用户的自然语言查询
- 通过思考-行动循环，自主决定是否需要调用工具
- 调用工具获取信息后，综合给出最终答案

### 1.2 核心架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      SmartAgent (LangGraph)                  │
│                                                              │
│   ┌──────────┐     ┌──────────────┐     ┌──────────┐        │
│   │  think    │────▶│ execute_tools │────▶│  think    │───▶   │
│   │  (节点)   │     │    (节点)     │     │  (节点)   │       │
│   └────┬─────┘     └──────────────┘     └────┬──────┘       │
│        │                                      │              │
│        ▼                                      ▼              │
│   ┌──────────┐                          ┌──────────┐        │
│   │  __end__  │◀────────────────────────│  __end__  │        │
│   │ (无工具)  │                         │ (超兜底)  │        │
│   └──────────┘                          └──────────┘        │
│                                                              │
│   ┌──────────────────────────────────────────────────┐       │
│   │  路由逻辑 _route_after_think：                    │       │
│   │  - 有工具调用 → execute_tools（继续循环）           │       │
│   │  - 无工具调用 → __end__（LLM 直接给出最终答案）      │       │
│   │  - 超最大迭代 → __end__（兜底保护）                 │       │
│   └──────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 工作流说明

| 阶段 | 节点 | 职责 |
|------|------|------|
| **思考** | `think` | LLM 绑定工具，流式输出内容。需要工具时输出工具调用；不需要时输出就是最终答案 |
| **执行** | `execute_tools` | 并发执行所有工具调用，返回结果到消息历史 |
| **路由** | `_route_after_think` | 根据是否有工具调用决定走向：有工具 → 继续循环；无工具 → 结束 |

---

## 二、核心数据模型

### 2.1 AgentState（LangGraph 状态）

```python
class AgentState(TypedDict):
    user_input: str          # 用户原始输入
    messages: List[Any]      # 完整的消息历史（SystemMessage, HumanMessage, AIMessage, ToolMessage）
    thinking: str            # 累积的推理过程
    tool_results: List[str]  # 工具执行结果列表
    iteration: int           # 当前迭代次数（防死循环）
    final_answer: str        # 最终答案（空串表示仍需继续循环）
    deep_thinking: bool      # 是否开启深度思考模式
    user_id: Optional[str]   # 当前登录用户 ID（由 Java 端传入）
```

### 2.2 StreamEvent（流式事件）

```python
@dataclass
class StreamEvent:
    event_type: str          # 事件类型："reasoning" | "token" | "tool_call" | "done"
    content: str             # 事件内容
    tool_name: str           # 工具名（tool_call 事件时使用）
    tool_args: dict          # 工具参数（tool_call 事件时使用）
    reasoning_content: str   # 深度思考模式下的推理链内容
```

**事件流转生命周期：**

```
用户消息
  │
  ▼
think 节点 ──流式输出──→ token/reasoning 事件（实时推送前端）
  │
  ├── 有工具调用 → tool_call 事件 → execute_tools → think（循环）
  │
  └── 无工具调用 → done 事件（携带最终答案）→ 结束
```

---

## 三、LLM 配置体系

### 3.1 双 LLM 实例设计

| 实例 | 用途 | temperature | extra_body | 说明 |
|------|------|------------|------------|------|
| `self.llm` | 普通模式 | 0.3 | `thinking: disabled` | LangChain ChatOpenAI，流式输出 |
| `self.llm_deep` | 深度思考模式 | 0.1 | `thinking: enabled` | LangChain ChatOpenAI，但深度模式下不直接使用 |
| `self.raw_deep_client` | 深度思考模式 | — | `thinking: enabled` | 原生 OpenAI 客户端（异步），绕过 LangChain |

### 3.2 为什么深度思考模式需要原生客户端？

DeepSeek 的 `reasoning_content` 字段是**非标准 OpenAI 字段**，LangChain 的 ChatOpenAI 在流式模式下会丢失此字段。深度思考模式下需要满足 DeepSeek API 的要求：**在工具调用消息历史中回传 `reasoning_content`**，否则 API 返回 400 错误。

因此深度思考模式走原生客户端流程：

```
LangChain messages ──→ _to_openai_messages() ──→ OpenAI dict 格式
                                                      │
                                                      ▼
                                               raw_deep_client
                                               (原生流式调用)
                                                      │
                                                      ▼
                                          手动解析 delta 中的:
                                          - reasoning_content → reasoning 事件
                                          - content → token 事件
                                          - tool_calls → 聚合 → tool_call 事件
```

---

## 四、核心节点实现

### 4.1 think 节点（普通模式）

```python
_think_node(state)
  │
  ├── deep_thinking=False → _think_node_normal(state)
  └── deep_thinking=True  → _think_node_deep(state)
```

#### _think_node_normal

```
1. llm_with_tools = self.llm.bind_tools(self.tools)
2. 流式调用: async for chunk in llm_with_tools.astream(messages)
3. 累积 content → 发射 token 事件
4. 获取 tool_calls
5. 调用 _emit_then_build_think_result 处理路由
```

#### _think_node_deep

```
1. openai_messages = _to_openai_messages(state["messages"])
   - 关键：将 LangChain AIMessage 的 additional_kwargs.reasoning_content
     回传到 OpenAI 格式消息中
     
2. 调用原生 API:
   response = await self.raw_deep_client.chat.completions.create(
       messages=openai_messages,
       tools=self._openai_tools,     # 预转换的 OpenAI 工具格式
       stream=True,
       extra_body={"thinking": {"type": "enabled"}},
   )
   
3. 流式解析 delta:
   │
   ├── delta.reasoning_content → 累积 → 发射 reasoning 事件
   ├── delta.content → 累积 → 发射 token 事件
   └── delta.tool_calls → 按 index 聚合 → raw_tool_calls dict
   
4. 将 raw_tool_calls 转换为标准格式:
   tool_calls = [{"id": "...", "name": "...", "args": {...}}]
   
5. 调用 _emit_then_build_think_result 处理路由
```

### 4.2 共享方法 _emit_then_build_think_result

这是 ReAct 模式的核心决策点：

```
_emit_then_build_think_result(state, accumulated_content, accumulated_reasoning, tool_calls)
  │
  ├── tool_calls 非空（有工具调用）:
  │   ├── 发射 tool_call 事件（每个工具一个事件）
  │   ├── 构造带 tool_calls 的 AIMessage
  │   │   - additional_kwargs 中保留 reasoning_content（深度思考模式）
  │   ├── 追加到 messages 历史
  │   └── 返回 {"final_answer": ""}  → 路由继续循环
  │
  └── tool_calls 为空（无工具调用）:
      ├── 不追加 AIMessage（保持本轮分析结果简洁）
      └── 返回 {"final_answer": accumulated_content}  → 路由走向 __end__
```

### 4.3 execute_tools 节点

```python
_execute_tools_node(state)
  │
  1. 取 messages[-1] 中的 tool_calls
  2. 遍历 tool_calls，对每个调用:
     - 在 self.tools 中按 name 匹配
     - 执行 tool.ainvoke(tool_args)
  3. 使用 asyncio.gather 实现并发执行
  4. 每个结果包装为 ToolMessage（含 tool_call_id）
  5. 追加到 messages 历史
```

**并发执行示意：**

```
tool_calls = [tc1, tc2, tc3]
                  │
                  ▼
asyncio.gather(
    _execute_single(tc1),  ←── tool1.ainvoke(args1)
    _execute_single(tc2),  ←── tool2.ainvoke(args2)
    _execute_single(tc3),  ←── tool1.ainvoke(args3)  # 同一工具也可并发
)
                  │
                  ▼
        [ToolMessage, ToolMessage, ToolMessage]
```

### 4.4 路由函数 _route_after_think

```python
_route_after_think(state) → Literal["execute_tools", "__end__"]

逻辑：
1. 检查 iteration >= max_iter (默认10)
   - 是 → "__end__"（兜底保护，防止无限循环烧穿 token）
2. 检查 messages[-1] 是否有 tool_calls
   - 有 → "execute_tools"（继续 ReAct 循环）
   - 无 → "__end__"（LLM 已直接回答）
```

---

## 五、工具系统集成

### 5.1 工具注册

所有工具在 [tools/__init__.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/__init__.py) 中集中注册：

| 工具分组 | 包含工具 | 来源文件 |
|---------|---------|---------|
| **文章搜索** | `search_articles_by_keyword`, `get_hot_articles`, `get_article_by_id`, `get_article_content_by_id` | [article_tool.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/article_tool.py) |
| **向量检索** | `search_knowledge_base` | [vector_tool.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/vector_tool.py) |
| **博主探索** | `search_authors_by_keyword`, `get_hot_authors`, `get_author_by_id` | [author_tool.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/author_tool.py) |
| **博客元数据** | `get_category_list`, `get_hot_tag_list` | [blog_tool.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/blog_tool.py) |
| **实用工具** | `get_the_time`, `get_current_user_id` | [common_tool.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/common_tool.py), [user_tool.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/user_tool.py) |
| **Skill 工具** | `get_skill_details`, `list_available_skills` | [skill_tool.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/skill_tool.py) |
| **外部搜索** | `search_external_tech_blogs`（需 Tavily API Key） | [article_tool.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/tools/article_tool.py) |

### 5.2 动态工具加载

```python
def _get_tools_with_tavily():
    tools = _base_tools.copy()
    if get_settings().tavily_api_key:  # 有 API Key 才加载外部搜索
        tools.extend(_tavily_tools)
    return tools
```

### 5.3 预转换工具格式

在 SmartAgent 初始化时，将 LangChain 工具列表一次性转换为 OpenAI 格式，避免每次深度思考调用时重复转换：

```python
self._openai_tools = [convert_to_openai_tool(t) for t in ALL_TOOLS]
```

---

## 六、渐进式披露（Progressive Disclosure）与 Skills 体系

### 6.1 设计动机

系统提示词中直接包含所有工具和 Skills 的完整描述会导致 token 消耗过大。渐进式披露策略：**只注入简要描述，Agent 在需要时通过工具获取详情**。

### 6.2 两阶段披露

```
阶段一（系统提示词）：
  Agent 启动时，系统提示词中只包含：
  - 工具列表（名称 + 简要 description）
  - Skills 简要描述（名称 + 一行说明）
  - 一条重要指令："匹配到 Skill 时必须先调用 get_skill_details"

阶段二（按需加载）：
  当 Agent 判断用户需求匹配某个 Skill 时：
  1. 调用 get_skill_details(skill_name) 获取完整 Skill 说明
  2. Skill 说明中包含：触发条件、工具调用顺序、工作流程、示例
  3. 按 Skill 指南调用具体工具
```

### 6.3 Skills 目录结构

```
skills/
├── loader.py                 # Skill 加载器
├── article-search/
│   └── SKILL.md              # 文章搜索技能
├── author-discovery/
│   └── SKILL.md
├── knowledge-qa/
│   └── SKILL.md
├── smart-chat/
│   └── SKILL.md
└── smart-search/
    └── SKILL.md
```

### 6.4 Agent 系统提示词中的 Skill 指南

```python
### 你的工作流程

1. **分析用户需求** — 判断用户意图，匹配到对应的 Skill
2. **获取 Skill 详情（重要）**：
   - 如果匹配到某个 Skill，**必须先调用 `get_skill_details(skill_name)` 获取详细的工作流程和工具使用指南**
   - 如果不确定有哪些 Skills，调用 `list_available_skills()` 查看所有可用技能
3. **决定行动**：
   - 不需要工具 → 直接给出最终答案
   - 需要工具 → 按 Skill 指南调用工具
4. **给出最终答案**
```

---

## 七、流式调用入口

### 7.1 astream_chat_with_result

```python
async def astream_chat_with_result(
    self, message: str, deep_thinking: bool = False, user_id: str | None = None
) -> AsyncGenerator[StreamEvent, None]:
```

**调用流程：**

```
1. 初始化：
   - 创建 asyncio.Queue 作为事件队列
   - 清空 _tool_call_infos
   - 根据 deep_thinking 选择 system_prompt
   
2. 构造初始状态 AgentState：
   - messages = [SystemMessage, HumanMessage]
   - iteration = 0
   - final_answer = ""
   
3. 用户身份注入：
   - if user_id: set_current_user_id(user_id)
   - 工具中通过 get_current_user_id() 读取
   
4. 异步执行（后台任务）：
   - run_graph() → graph.ainvoke(initial_state)
   - 图中各节点通过 self._event_queue 发射事件
   - 执行完成后发射 done 事件
   
5. 事件消费（主协程）：
   while True:
       event = await self._event_queue.get()
       if event.event_type == "done":
           yield event
           break
       yield event
```

### 7.2 前端事件消费示例

```python
# 伪代码：前端消费端
async for event in agent.astream_chat_with_result("你好", deep_thinking=True):
    if event.event_type == "reasoning":
        # 展示推理链（打字机效果）
        update_reasoning(event.content)
    elif event.event_type == "token":
        # 展示最终答案（打字机效果）
        update_answer(event.content)
    elif event.event_type == "tool_call":
        # 展示工具调用状态
        show_tool_call(event.tool_name, event.tool_args)
    elif event.event_type == "done":
        # 流式结束，获取完整结果
        final_answer = event.content
        final_thinking = event.reasoning_content
```

---

## 八、单例模式

```python
_smart_agent: Optional[SmartAgent] = None

def get_smart_agent() -> SmartAgent:
    """获取智能 Agent 单例"""
    global _smart_agent
    if _smart_agent is None:
        _smart_agent = SmartAgent()
    return _smart_agent
```

---

## 九、关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| **状态管理** | LangGraph StateGraph + MemorySaver | 天然支持循环图，检查点机制实现状态持久化 |
| **深度思考模式** | 原生 OpenAI 客户端（绕过 LangChain） | 流式场景下 LangChain 丢失 DeepSeek 的 reasoning_content 字段 |
| **工具并发** | asyncio.gather | 多工具调用时互不阻塞，提升响应速度 |
| **工具格式** | 预转换（convert_to_openai_tool） | 避免每次深度思考调用时重复转换，提升性能 |
| **消息转换** | 手动构造 OpenAI dict | 精确控制 reasoning_content 回传，满足 DeepSeek API 校验 |
| **渐进式披露** | 按需加载 Skill 详情 | 减少系统提示词 token 消耗，支持大量 Skills |
| **用户身份隔离** | 全局变量 + set_current_user_id | 工具中通过 get_current_user_id 读取，确保数据权限 |
| **兜底保护** | max_iterations = 10 | 防止无限 ReAct 循环烧穿 token 预算 |

---

## 十、与外部系统的集成

### 10.1 与 Java 后端的协作

```
用户请求 (Java 端)
  │
  ├── message: 用户消息
  ├── deep_thinking: true/false
  └── user_id: 当前登录用户 ID
       │
       ▼
Python SmartAgent (LangGraph)
       │
       ▼
StreamEvent 流式返回 (SSE)
  ├── reasoning 事件 → 推理链展示
  ├── token 事件     → 打字机效果
  ├── tool_call 事件  → 工具调用状态
  └── done 事件      → 最终结果
       │
       ▼
Java 端消费并转发给前端
```

### 10.2 用户身份隔离机制

```
Java 端传入 user_id
  │
  ▼
SmartAgent.astream_chat_with_result(user_id=user_id)
  │
  ├── 注入工具上下文:
  │   set_current_user_id(user_id)
  │
  └── 工具调用时读取:
      get_current_user_id() → user_id
      │
      ▼
  API 请求携带 user_id → Java 后端按用户权限过滤数据
```
