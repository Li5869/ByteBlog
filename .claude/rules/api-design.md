# API 设计约定

## 总体设计原则

- 采用 RESTful API 设计风格
- 资源通过 URL 路径标识，操作通过 HTTP 方法体现
- 统一的请求和响应格式
- 前后端使用 JSON 进行数据交换

---

## RESTful API 规范

### 基础路径
```
/api/{模块}/{资源}
```

### HTTP 方法与操作映射

| HTTP 方法 | 操作 | 示例 |
|-----------|------|------|
| `GET` | 查询/获取 | `GET /api/article/articles` — 获取文章列表 |
| `GET` | 查询详情 | `GET /api/article/articles/{id}` — 获取文章详情 |
| `POST` | 创建 | `POST /api/article/articles` — 创建文章 |
| `PUT` | 更新 | `PUT /api/article/articles/{id}` — 更新文章 |
| `DELETE` | 删除 | `DELETE /api/article/articles/{id}` — 删除文章 |

### 路径命名规范
- 使用小写字母
- 多单词使用连字符分隔：`/api/user/user-profiles`
- 资源名使用复数形式：`/api/article/articles`、`/api/comment/comments`
- 路径参数使用 `{id}` 占位符

### 查询参数规范
- 分页参数：`?current=1&size=10`
- 过滤条件：`?status=published&categoryId=5`
- 排序：`?sortBy=createdAt&sortOrder=desc`
- 搜索：`?keyword=搜索关键词`

### GET 请求 DTO 规范
- GET 请求参数超过 3 个时，必须定义 DTO 进行传输
- DTO 放在 blog-common 或对应模块的 dto 包下
- 使用 `@Schema(description = "...")` 注解描述
  ```java
  @GetMapping("/articles")
  public JsonData<Page<ArticleListVO>> getArticlePage(ArticleQueryDTO dto) {
      // 使用 DTO 接收参数
  }
  
  @Data
  @Schema(description = "文章列表查询参数")
  public class ArticleQueryDTO {
      private Integer current;
      private Integer size;
      private Long categoryId;
      private Long tagId;
      private String orderBy;
  }
  ```

---

## 统一响应格式

### 标准响应
```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务状态码，0 表示成功，非 0 表示失败 |
| `msg` | string | 提示信息，成功为 "success"，失败为具体错误描述 |
| `data` | object | 响应数据，可为任意 JSON 类型 |

### 成功响应示例
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "title": "文章标题",
    "content": "文章内容"
  }
}
```

### 错误响应示例
```json
{
  "code": 1001,
  "msg": "用户不存在",
  "data": null
}
```

### Java 端实现
```java
// 统一使用 JsonData<T> 返回
return JsonData.buildSuccess(data);        // 成功，带数据
return JsonData.buildSuccess();             // 成功，无数据
return JsonData.buildResult(BizCodeEnum.LOGIN_ERROR);  // 业务错误

// Controller 层
@GetMapping("/{id}")
public JsonData<ArticleDetailVO> getArticleDetail(@PathVariable Long id) {
    ArticleDetailVO vo = articleService.getArticleDetail(id);
    return JsonData.buildSuccess(vo);
}
```

---

## 分页响应格式

### 请求参数
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `current` | int | 1 | 当前页码 |
| `size` | int | 10 | 每页条数（最大 50） |

### 响应格式
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "records": [],
    "total": 100,
    "current": 1,
    "size": 10
  }
}
```

### Java 端实现
```java
@GetMapping("/list")
public JsonData<Page<ArticleListVO>> getArticlePage(ArticleQueryDTO dto) {
    Page<ArticleListVO> page = articleService.getArticlePage(dto);
    return JsonData.buildSuccess(page);
}
```

---

## 跨模块 API 调用规范

### 接口定义
- 跨模块调用的 API 接口定义在 `blog-api` 模块中
- 服务提供方实现 API 接口
- 服务消费方依赖 `blog-api` 模块，注入 API 接口使用

### 接口示例
```java
// blog-api 模块定义接口
public interface UserInfoApi {
    List<UserDTO> getUserInfo(Collection<Long> userIds);
}

// blog-security 模块实现接口
@Service
public class UserServiceImpl implements IUserService, UserInfoApi {
    @Override
    public List<UserDTO> getUserInfo(Collection<Long> userIds) {
        // 实现逻辑
    }
}

// blog-article 模块使用接口
@Service
public class ArticleServiceImpl {
    private final UserInfoApi userInfoApi;
    
    public void someMethod() {
        List<UserDTO> users = userInfoApi.getUserInfo(userIds);
    }
}
```

### 跨模块 API 列表

| 接口 | 提供方模块 | 用途 |
|------|-----------|------|
| `UserInfoApi` | blog-security | 获取用户信息 |
| `LikeApi` | blog-interaction | 获取点赞数和点赞状态 |
| `FollowApi` | blog-interaction | 获取关注状态 |
| `SearchSyncApi` | blog-search | 同步数据到 ES 索引 |
| `DeleteSearchAPI` | blog-search | 删除 ES 索引 |
| `NotificationApi` | blog-interaction | 发送通知 |

---

## Python 端 API 规范

### FastAPI 路由规范
- 路由前缀：`/api/v1/{模块}`
- 使用 Pydantic 模型进行请求验证
- 统一响应格式与 Java 后端保持一致
- 支持 SSE 流式响应

### 请求/响应模型
```python
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/rag", tags=["RAG"])

class RAGQueryRequest(BaseModel):
    question: str
    session_id: str | None = None
    top_k: int = 5

class ApiResponse(BaseModel):
    code: int = 0
    msg: str = "success"
    data: dict

@router.post("/query", response_model=ApiResponse)
async def query(request: RAGQueryRequest):
    # 处理逻辑
    return ApiResponse(data={"answer": "..."})
```

### 统一响应 Python 实现
```python
from pydantic import BaseModel
from typing import Any

class ApiResponse(BaseModel):
    code: int = 0
    msg: str = "success"
    data: Any = None
```

### SSE 流式响应规范
```python
from fastapi.responses import StreamingResponse

@router.post("/stream")
async def stream_query(request: QueryRequest):
    async def event_generator():
        async for chunk in agent.arun_stream(request.question):
            yield f"data: {json.dumps(chunk, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"
    
    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )
```

---

## 认证规范

### JWT Token
- 存储位置：请求头 `token: {token}`
- Token 有效期：30 分钟（Redis 存储）
- 刷新机制：活跃用户自动续期

### 用户上下文获取
```java
// 方式一：UserContextHolder（推荐）
Long userId = UserContextHolder.getUserId();

// 方式二：SecurityContextHolder
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
LoginUser loginUser = (LoginUser) auth.getPrincipal();
```

### 前端 Token 传递
```js
// 统一在请求封装中自动携带
fetch(url, {
    headers: {
        'token': getToken(),
        'Content-Type': 'application/json'
    }
})
```

---

## API 文档规范

### Controller 类注解
```java
@Tag(name = "文章管理", description = "文章的增删改查接口")
@RestController
@RequestMapping("/api/article/articles")
@RequiredArgsConstructor
public class ArticleController {
```

### 接口方法注解
```java
@Operation(summary = "获取文章详情", description = "根据文章 ID 获取完整文章信息，包含作者、分类、标签等")
@GetMapping("/{id}")
public JsonData<ArticleDetailVO> getArticleDetail(@PathVariable Long id) {
    return JsonData.buildSuccess(articleService.getArticleDetail(id));
}
```
