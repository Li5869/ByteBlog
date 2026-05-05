# WritingAgent 技术实现文档

## 一、架构总览

### 1.1 职责定位

**WritingAgent** 是一个基于 **LangGraph StateGraph** 构建的**AI 智能写作 Agent**，采用 **Plan-and-Execute** 范式。它的核心职责是：

- 分析用户的写作需求，生成结构化的**写作计划**
- 按计划并行生成文章的**标题、标签分类、摘要、正文**
- 自动评估文章质量，根据评分进行**自动修订**
- 支持**中断/恢复**模式：先生成计划等待用户确认，再执行写作

### 1.2 核心架构图

```
┌──────────────────────────────────────────────────────────────────┐
│                    WritingAgent (LangGraph)                       │
│                                                                  │
│    ┌──────────┐     ┌──────────┐     ┌──────────┐               │
│    │   plan    │────▶│ execute  │────▶│ reflect  │──┐            │
│    │  (规划)   │     │  (执行)   │     │  (反思)   │  │            │
│    └──────────┘     └──────────┘     └──────────┘  │            │
│                                                    │            │
│         ▲                          ┌───────────────┘            │
│         │                          │                            │
│         │              ┌───────────▼──────────┐                  │
│         │              │  _route_after_reflection │              │
│         │              │  score < 阈值 → retry    │              │
│         │              │  score ≥ 阈值 → proceed  │              │
│         │              └───────────────────────┘                  │
│         │                          │                             │
│         │                          ▼                             │
│         │              ┌───────────┐                             │
│         └──────────────│ finalize  │                             │
│        revise_plan     │  (终态)   │                             │
│                        └─────┬─────┘                             │
│                              │                                   │
│                    ┌─────────▼──────────┐                        │
│                    │ _route_after_finalize │                     │
│                    │ action=complete → END │                     │
│                    │ action=revise_plan → plan                    │
│                    └───────────────────┘                         │
└──────────────────────────────────────────────────────────────────┘
```

### 1.3 四阶段工作流

| 阶段 | 节点 | 职责 | LLM 角色 | Temperature |
|------|------|------|---------|-------------|
| **规划** | `plan` | 分析写作需求，生成写作计划（topic, key_points, structure 等） | **Planner**（规划师） | **0.1**（低温，保证计划精确） |
| **执行** | `execute` | 按计划并行生成标题+标签 → 摘要 → 正文 | **Writer**（写手）+ **Classifier**（分类器） | **0.6**（中高温，提升创造力）+ **0.2**（较低温，确定性强） |
| **反思** | `reflect` | 评估文章质量，评分不达标时自动微调 | **Critic**（审稿人） | **0.1**（低温，评分客观一致） |
| **终态** | `finalize` | 整理写作结果，标记任务完成 | — | — |

---

## 二、核心数据模型

### 2.1 WritingAgentState（LangGraph 工作流上下文）

```python
class WritingAgentState(TypedDict):
    user_request: str                           # 用户原始写作需求
    plan: NotRequired[Optional[WritingPlan]]    # 写作计划
    plan_approved: NotRequired[Optional[bool]]  # 计划是否已确认
    plan_feedback: NotRequired[Optional[str]]   # 用户对计划的修改意见
    current_step: NotRequired[Optional[str]]    # 当前步骤标识
    writing_result: NotRequired[Optional[WritingResult]]  # 写作结果
    references: NotRequired[Optional[List[dict]]]         # 参考资料列表
    reflection: NotRequired[Optional[ReflectionResult]]   # 评估结果
    revision_count: NotRequired[int]            # 已修订次数
    article_id: NotRequired[Optional[str]]      # 文章 ID
    action: NotRequired[Optional[str]]          # 用户操作指令
    cover: NotRequired[Optional[str]]           # 封面信息
    error: NotRequired[Optional[dict]]          # 错误信息
```

### 2.2 WritingPlan（写作计划 DTO）

```python
class WritingPlan(BaseModel):
    topic: str                    # 文章核心主题
    target_audience: str          # 目标读者群体
    key_points: List[str]         # 核心要点（3-5个）
    writing_style: str            # 写作风格（教程/科普/经验分享/深度分析/技术解读/随笔）
    estimated_length: str         # 预计篇幅（短文800字/中文1500字/长文3000字+）
    reference_keywords: List[str] # 搜索关键词（3-5个）
    structure: List[str]          # 文章结构大纲（章节标题列表）
```

### 2.3 WritingResult（写作结果 VO）

```python
class WritingResult(BaseModel):
    title: str                              # 文章标题
    summary: str                            # 文章摘要
    content: str                            # 文章正文（Markdown 格式）
    category_name: Optional[str]            # 分类名称
    category_id: Optional[int]              # 分类 ID
    tag_names: List[str]                    # 标签名称列表（新标签）
    tag_ids: List[int]                      # 标签 ID 列表（已有标签）
```

### 2.4 ReflectionResult（反思结果 VO）

```python
class ReflectionResult(BaseModel):
    score: float                # 综合评分（满分10分）
    completeness: float         # 完整性评分（权重30%）
    structure: float            # 结构性评分（权重20%）
    expression: float           # 表达质量评分（权重25%）
    practicality: float         # 实用性评分（权重15%）
    format: float               # 格式规范评分（权重10%）
    strengths: List[str]        # 优点列表
    weaknesses: List[str]       # 不足列表
    suggestions: List[str]      # 改进建议列表
    revised_content: Optional[str]  # 修订后的内容
```

**评分权重矩阵：**

| 维度 | 权重 | 评分标准 |
|------|------|----------|
| 完整性 | 30% | 8-10分：覆盖所有核心要点，有深入阐述 |
| 表达质量 | 25% | 8-10分：语言流畅，术语准确，无冗余 |
| 结构性 | 20% | 8-10分：章节清晰，逻辑严密，过渡自然 |
| 实用性 | 15% | 8-10分：对目标读者有直接帮助，可操作性强 |
| 格式规范 | 10% | 8-10分：Markdown 格式完美，代码块正确 |

---

## 三、LLM 四角色配置体系

### 3.1 角色架构

```
WritingAgent
  │
  ├── self.llm_planner     (temperature=0.1)  ──→ WritingPlan 节点
  │                                                  │
  ├── self.llm_writer      (temperature=0.6)  ──→ WritingContentService
  │                                                  ├── generate_title()
  │                                                  ├── generate_summary()
  │                                                  └── generate_content()
  │
  ├── self.llm_critic      (temperature=0.1)  ──→ WritingQualityService
  │                                                  ├── evaluate()
  │                                                  └── revise()
  │
  └── self.llm_classifier  (temperature=0.2)  ──→ WritingTagService
                                                      └── generate_tags()
```

### 3.2 角色温度设计策略

| 角色 | Temperature | 对应节点/服务 | 设计理由 |
|------|------------|-------------|---------|
| **Planner** | 0.1 | plan 节点 | 低温保证计划精确、结构清晰，减少无关内容 |
| **Writer** | 0.6 | WritingContentService | 中高温提升内容创造力、语言生动性，避免千篇一律 |
| **Critic** | 0.1 | WritingQualityService | 低温确保评分标准一致、客观公正 |
| **Classifier** | 0.2 | WritingTagService | 较低温保证分类结果确定、可复现 |

---

## 四、业务服务层

### 4.1 服务架构

```
services/
└── business/
    └── writing/
        ├── writing_content_service.py    # 内容生成服务
        ├── writing_tag_service.py        # 标签分类服务
        └── writing_quality_service.py    # 质量评估服务
```

### 4.2 WritingContentService（内容生成服务）

**职责**：根据写作计划生成文章标题、摘要、正文。

| 方法 | 输入 | 输出 | 依赖 |
|------|------|------|------|
| `generate_title(plan)` | WritingPlan | str（标题） | StrOutputParser |
| `generate_summary(plan, title)` | WritingPlan, title | str（摘要） | StrOutputParser |
| `generate_content(plan, title, summary, references)` | WritingPlan, title, summary, references | str（正文 Markdown） | StrOutputParser |

**调用链：** `llm_writer | StrOutputParser() → chain.ainvoke(prompt)`

### 4.3 WritingTagService（标签分类服务）

**职责**：生成文章标签和分类，并与数据库中已有的分类/标签进行匹配。

```
generate_tags(plan)
  │
  ├── 1. 并发获取数据库已有分类和标签
  │      asyncio.gather(
  │          blog_service.get_categories(),
  │          blog_service.get_tags()
  │      )
  │
  ├── 2. LLM 生成推荐标签和分类
  │      structured_generate(prompt, TagGenerationResponse, llm_classifier)
  │      → TagGenerationResponse { category: str, tags: List[str] }
  │
  ├── 3. 匹配已有分类
  │      _match_category(name, categories) → category_id
  │      - 精确匹配名称，返回 ID
  │
  └── 4. 匹配已有标签
         _match_tags(names, existing_tags) → (tag_ids, new_tag_names)
         - 已在库中 → 返回 ID
         - 不在库中 → 作为新标签名返回
```

### 4.4 WritingQualityService（质量评估与修订服务）

**职责**：评估文章质量，根据评估结果微调文章内容。

```
evaluate(plan, result)
  │
  ├── 构建评估 prompt（含评分标准和文章内容）
  ├── structured_generate(prompt, ReflectionResult, llm_critic)
  └── 返回包含五维评分 + 优缺点 + 建议的评估结果

revise(result, reflection)
  │
  ├── 提取 weakness 和 suggestion
  ├── 构建修订 prompt
  ├── llm_critic | StrOutputParser() → chain.ainvoke(prompt)
  └── 返回更新 content 后的 WritingResult
```

---

## 五、核心节点实现细节

### 5.1 plan 节点

```
_plan_node(state)
  │
  ├── 判断是否有 plan_feedback（是否是修订流程）
  │   ├── 有 feedback → get_writing_revision_prompt(user_request, original_plan, feedback)
  │   └── 无 feedback → get_writing_plan_prompt(user_request)
  │
  ├── structured_generate(prompt, WritingPlan, llm_planner)
  │   - 使用 DeepSeek JSON Mode (response_format={"type": "json_object"})
  │   - 自动重试机制（最多3次）
  │
  ├── 如果有 reference_keywords → 搜索参考资料
  │   _search_references(keywords, topic)
  │
  └── 返回: {"plan": plan, "references": references, "current_step": "plan_generated"}
```

### 5.2 execute 节点

**核心优化：三阶段并行与串行混合策略**

```
_execute_node(state)
  │
  ├── 如果 current_step == "revised"（从 reflect 重试进入）:
  │   └── 跳过重新生成，直接返回已有 writing_result
  │
  ├── Phase 1 ── 并行（依赖 plan）:
  │   asyncio.gather(
  │       content_service.generate_title(plan),    # 生成标题
  │       tag_service.generate_tags(plan),          # 生成标签+分类
  │   )
  │   ↓
  │   result.title = title_val
  │   result.category_name/ID = tag_result
  │   result.tag_names/IDs = tag_result
  │
  ├── Phase 2 ── 串行（依赖 title）:
  │   result.summary = content_service.generate_summary(plan, title)
  │
  ├── Phase 3 ── 串行（依赖 title + summary）:
  │   result.content = content_service.generate_content(plan, title, summary, references)
  │
  └── 每个子步骤通过 progress_callback 推送 phase/token 事件
```

**依赖关系图：**

```
            ┌──────────────┐
            │     plan     │
            └──────┬───────┘
                   │
         ┌─────────┴─────────┐
         ▼                   ▼
    ┌────────┐         ┌──────────┐
    │ title  │         │   tags   │    ← Phase 1（并行）
    └───┬────┘         └──────────┘
        │
        ▼
    ┌──────────┐
    │ summary  │                          ← Phase 2（串行，依赖 title）
    └───┬──────┘
        │
        ▼
    ┌──────────┐
    │ content  │                          ← Phase 3（串行，依赖 title+summary）
    └──────────┘
```

### 5.3 reflect 节点

```
_reflect_node(state)
  │
  ├── 1. quality_service.evaluate(plan, result)
  │      → ReflectionResult (含完整五维评分)
  │
  ├── 2. 判断是否需要修订:
  │      if score < reflection_threshold(7.0) AND revision_count < max_revisions(3):
  │          → quality_service.revise(result, reflection)
  │          → 返回 revised_result
  │          → revision_count + 1
  │          → current_step = "revised"（标记为修订状态）
  │      else:
  │          → 返回原始 result
  │          → current_step = "reflected"
  │
  └── 返回评估结果 + 可能修订后的结果
```

### 5.4 finalize 节点

```
_finalize_node(state)
  │
  ├── 从 state 中取出 writing_result
  ├── 标记 current_step = "finalized"
  └── 返回整理后的结果（不调用任何保存接口，由前端直调 Java 后端保存）
```

---

## 六、路由与决策逻辑

### 6.1 _route_after_reflection（反思后路由）

```python
_route_after_reflection(state) → "retry" | "proceed"

逻辑：
1. reflection.score < reflection_threshold(7.0)
   AND revision_count < max_revisions(3)
   → "retry"（返回 execute 节点重试修订）

2. 否则 → "proceed"（进入 finalize 节点）
```

### 6.2 _route_after_finalize（终态确认后路由）

```python
_route_after_finalize(state) → "complete" | "revise_plan"

逻辑：
1. state.action == "revise_plan"
   → "revise_plan"（返回 plan 节点重新规划）

2. 否则 → "complete"（结束工作流）
```

### 6.3 完整决策树

```
用户输入
  │
  ▼
plan ──→ execute ──→ reflect
                        │
                 ┌──────┴──────┐
                 ▼              ▼
              retry           proceed
                 │              │
                 ▼              ▼
              execute        finalize
                               │
                        ┌──────┴──────┐
                        ▼              ▼
                     complete      revise_plan
                        │              │
                        ▼              ▼
                       END           plan（重新规划）
```

---

## 七、参考资料搜索集成

### 7.1 智能搜索策略

```
_search_references(keywords, topic)
  │
  ├── 智能搜索（smart_search_references 工具）:
  │   1. 站内搜索（循环 keywords）
  │      search_articles_by_keyword(keyword)
  │   2. 如果站内结果 < 3 条 → 外部搜索补充
  │      search_external_tech_blogs(query)  （需 Tavily API Key）
  │   3. 如果结果仍不足 → 用 topic 补充搜索
  │
  └── 兜底回退（_fallback_search）:
      - 只搜索站内
      - 每个关键词取前2条
```

### 7.2 搜索结果格式化

| 来源 | source 字段 | 格式化字段 |
|------|-----------|-----------|
| 站内文章 | `internal` | title, summary, author_name, views, url(/article/{id}), score |
| 外部博客 | `external` | title, content[:200], author, url, score |

---

## 八、中断与恢复机制

### 8.1 两阶段调用设计

WritingAgent 的公开入口分为两个阶段，利用 LangGraph 的 `interrupt_before` 机制实现：

```
阶段一：generate_plan(initial_state, thread_id)
  │
  ├── graph.astream(initial_state) 运行 plan 节点
  ├── 生成计划后自动停在 interrupt_before=["execute"] 中断点
  ├── 前端展示计划给用户确认
  └── 计划保存在 graph 检查点中

      [用户确认/修改]
           │
           ▼

阶段二：execute_stream(thread_id)
  │
  ├── graph.astream(None, config) 从检查点恢复执行
  ├── 自动运行 execute → reflect → (retry loop) → finalize → END
  ├── execute 节点的子步骤通过 progress_callback 推送实时进度
  │   - phase 事件：当前阶段（title/tags/summary/content）
  │   - token 事件：生成的内容片段
  ├── reflect 节点输出 reflection_result 事件
  └── finalize 节点输出 finalize_ready 事件
```

### 8.2 流式事件体系

```
WritingAgent 执行过程中通过 progress_callback 推送以下事件：

┌──────────────┬──────────────────────────────────────┐
│ 事件类型      │ 触发时机                              │
├──────────────┼──────────────────────────────────────┤
│ phase        │ execute 节点的每个子步骤开始            │
│              │ 取值：title/tags/summary/content      │
├──────────────┼──────────────────────────────────────┤
│ token        │ 每个子步骤的生成内容（实时流式输出）      │
├──────────────┼──────────────────────────────────────┤
│ plan_ready   │ plan 节点执行完成，计划已生成           │
├──────────────┼──────────────────────────────────────┤
│ reflection_result │ reflect 节点执行完成，评估结果已生成  │
├──────────────┼──────────────────────────────────────┤
│ finalize_ready    │ finalize 节点执行完成，写作结果已整理  │
├──────────────┼──────────────────────────────────────┤
│ done         │ 整个工作流执行完成                      │
├──────────────┼──────────────────────────────────────┤
│ error        │ 任意节点执行出错                        │
└──────────────┴──────────────────────────────────────┘
```

### 8.3 检查点持久化

```python
self.checkpointer = MemorySaver(serde=JsonPlusSerializer())
```

- 使用 `JsonPlusSerializer` 序列化状态，支持复杂类型
- `thread_id` 作为检查点标识，通常传入 Java 端生成的 `task_id`
- 中断点 `interrupt_before=["execute"]` 确保计划生成后等待用户确认

---

## 九、结构化生成工具

### 9.1 structured_generate（核心工具函数）

```python
async def structured_generate(
    prompt: str,
    output_model: Type[T],
    llm: ChatOpenAI,
    max_retries: int = 3,
) -> T:
```

**与 DeepSeek 的兼容性：**

使用 DeepSeek 官方推荐的 `json_object` 模式替代 OpenAI 专有的 `with_structured_output`（后者底层依赖 function/tool calling，DeepSeek 不支持）。

**实现流程：**

```
1. 从 output_model 的 JSON Schema 中提取字段定义
   - model_json_schema() → properties
   - 构建 schema_str（字段名、类型、描述）

2. 构造 SystemMessage + HumanMessage
   - SystemMessage: "请始终返回合法的 JSON 格式数据"
   - HumanMessage: 用户 prompt

3. 调用 llm.ainvoke(response_format={"type": "json_object"})
   - DeepSeek 强制输出合法 JSON

4. json.loads() 解析
   - 失败 → 重试（最多 max_retries 次）
   - 成功 → model_validate() 验证
   
5. 重试机制：
   - JSONDecodeError: 等待 1s 后重试
   - ValidationError: 等待 1s 后重试
   - 其他异常: 等待 2s 后重试
   - 全部失败 → 抛出 ValueError
```

### 9.2 使用 structured_generate 的节点

| 节点/服务 | 输出模型 | LLM 角色 |
|-----------|---------|---------|
| plan 节点 | WritingPlan | llm_planner |
| WritingTagService.generate_tags | TagGenerationResponse | llm_classifier |
| WritingQualityService.evaluate | ReflectionResult | llm_critic |

---

## 十、单例模式

```python
_writing_agent: Optional[WritingAgent] = None

def get_writing_agent() -> WritingAgent:
    """获取 Writing Agent 单例"""
    global _writing_agent
    if _writing_agent is None:
        _writing_agent = WritingAgent()
    return _writing_agent
```

---

## 十一、关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| **架构模式** | Plan-and-Execute | 先生成计划再执行，用户可对计划进行确认/修改，提升可控性 |
| **状态管理** | LangGraph StateGraph + MemorySaver(JsonPlusSerializer) | 支持复杂状态序列化，中断/恢复机制天然支持 |
| **LLM 角色分离** | 4 个独立 LLM 实例（不同 temperature） | 各节点职责不同，差异化温度参数提升各环节输出质量 |
| **执行优化** | Phase 1 并行 + Phase 2/3 串行 | title 和 tags 互不依赖可并行；summary 依赖 title，content 依赖 title+summary 必须串行 |
| **DeepSeek 兼容** | json_object 模式 + 手动 JSON Schema | DeepSeek 不支持 OpenAI 的 function calling 结构化输出 |
| **质量保障** | 五维评分 + 自动修订循环（最多3次） | 完整性30%、表达质量25%、结构性20%、实用性15%、格式规范10% |
| **参考搜索** | 智能搜索：站内优先 + 外部补充 + 兜底回退 | 优先使用站内已有内容，不足时自动补充外部高质量博客 |
| **中断/恢复** | interrupt_before=["execute"] + thread_id | 支持长时间写作流程，用户可在计划阶段确认/修改 |
| **数据保存** | Agent 只生产不保存 | 文章保存由前端直调 Java 后端，职责清晰 |
| **兜底保护** | max_revisions=3, reflection_threshold=7.0 | 防止无限修订循环，保证最低质量标准 |

---

## 十二、与外部系统的集成

### 12.1 完整的写作流程交互

```
用户（前端）               Java 后端               Python WritingAgent
    │                        │                         │
    │  1. 发起写作请求        │                         │
    │──────────────────────▶│                         │
    │                        │  2. 转发写作请求         │
    │                        │────────────────────▶   │
    │                        │                         │
    │                        │  3. generate_plan()      │
    │                        │    plan_ready 事件       │
    │                        │◀────────────────────    │
    │  4. 返回写作计划        │                         │
    │◀──────────────────────│                         │
    │                        │                         │
    │  5. 用户确认/修改       │                         │
    │──────────────────────▶│                         │
    │                        │  6. execute_stream()     │
    │                        │    phase/token 事件      │
    │                        │    reflection_result     │
    │                        │    finalize_ready        │
    │                        │◀────────────────────    │
    │  7. 流式展示写作进度     │                         │
    │◀──────────────────────│                         │
    │                        │                         │
    │  8. 用户保存文章        │                         │
    │──────────────────────▶│  (Java 后端保存到 DB)     │
```

### 12.2 与博客后端的交互

WritingTagService 通过 `BlogService` 与 Java 后端交互：

```
WritingTagService._fetch_existing_data()
  │
  ├── blog_service.get_categories()
  │   → GET /api/categories
  │   → [{id: 1, name: "Java"}, {id: 2, name: "Python"}, ...]
  │
  └── blog_service.get_tags()
      → GET /api/tags
      → [{id: 1, name: "Spring Boot"}, {id: 2, name: "微服务"}, ...]
```

### 12.3 API 请求/响应 DTO

写作 Agent 的 API 层定义在 [models/writing_models.py](file:///d:/code/Java/agent-project/ByteBlog/project-ai-agent/models/writing_models.py) 中：

| DTO | 用途 | 关键字段 |
|-----|------|---------|
| StartRequest | 启动写作任务 | task_id, message |
| ResumeRequest | 恢复写作任务 | action(approve/revise), feedback |
| StopRequest | 停止写作任务 | save_partial |
| FinalizeRequest | 完成写作任务 | action(complete/revise_plan) |
| CancelRequest | 取消写作任务 | reason |
