# 文章发布接口文档（project-front）

## 1. 目标与范围

本文档用于梳理 `project-front` 中“发布文章（/create-article）”流程所需接口，包含：

- 当前前后端已存在并可直接使用的接口
- 当前缺失但发布功能必需的接口（建议新增）
- 请求/响应字段规范与联调约定

## 2. 页面与数据来源分析

发布页：`src/views/CreateArticle.vue`

页面表单字段：

- `title`：文章标题（必填，前端限制 <= 200）
- `summary`：摘要（可选，前端限制 <= 500）
- `content`：Markdown 正文（必填）
- `cover`：封面 URL（可选，先上传图片后回填 URL）
- `categoryId`：分类 ID（可选）
- `tags`：标签 ID 数组（可选）

当前实现状态：

- 已实现：封面上传（调用 `/api/upload`）
- 未实现：保存草稿、发布文章（按钮目前为“功能开发中”）
- 分类/标签当前是本地写死数据，未接后端动态接口

## 3. 统一约定

### 3.1 网关前缀

- 前端 `BASE_URL = /api`
- 文档中的接口路径均为后端真实路径，不含 `/api` 前缀

### 3.2 鉴权

- 已登录请求头：`token: <JWT>`
- 未登录或过期：HTTP 401（前端会清理本地登录态并跳转首页）

### 3.3 统一响应

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

## 4. 已存在接口（可直接联调）

### 4.1 上传封面

- 方法：`POST`
- 路径：`/upload`
- Content-Type：`multipart/form-data`
- 鉴权：建议携带 `token`
- 入参：
  - `file`：图片文件
- 出参：`data` 为文件 URL 字符串

示例响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": "https://xxx/2026/04/04/abc123.jpg"
}
```

说明：

- 后端允许最大 10MB；前端当前限制 5MB
- 支持扩展名：jpg/jpeg/png/gif/bmp/webp 等

### 4.2 获取分类列表

- 方法：`GET`
- 路径：`/category`
- 鉴权：否
- 出参：分类数组（至少包含 `id`、`name`，并可包含 `articlesCount`）

### 4.3 获取我的文章列表（用于发布后回显、草稿管理）

- 方法：`GET`
- 路径：`/article/my-articles`
- 鉴权：是
- Query 参数：
  - `current`：页码，默认 1
  - `size`：每页条数，默认 10
  - `status`：0 草稿 / 1 已发布 / 2 已下架（可选）
  - `orderBy`：`created_at` / `likes` / `views`

### 4.4 获取文章详情（用于编辑回填可复用）

- 方法：`GET`
- 路径：`/article/articles/{id}`
- 鉴权：可选（登录后会返回点赞/收藏状态）
- 说明：当前返回包含 `content`、`category`、`tags`，可作为编辑页回填数据来源

## 5. 缺失接口（发布功能必需，建议新增）

当前后端 `blog-article` 模块尚无“创建文章/更新文章/草稿发布”接口。建议至少新增以下接口。

### 5.1 创建文章（草稿/发布）

- 方法：`POST`
- 路径：`/article/articles/publish`
- 鉴权：是
- 说明：通过 `status` 控制“保存草稿”或“直接发布”

请求体：

```json
{
  "title": "Spring Boot + Vue 实战",
  "summary": "从 0 到 1 搭建博客系统",
  "content": "# 正文 markdown",
  "cover": "https://xxx/cover.jpg",
  "categoryId": 1,
  "tagIds": [1, 3, 8],
  "status": 1
}
```

字段约束建议：

- `title`：必填，1-200
- `summary`：可选，0-500
- `content`：必填（发布时必须，草稿可允许空但建议最少 1 字）
- `categoryId`：可选
- `tagIds`：可选，去重后建议上限 10
- `status`：必填，`0`=草稿，`1`=发布

响应体（建议）：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10001,
    "status": 1
  }
}
```

### 5.2 更新文章（含草稿保存）

- 方法：`PUT`
- 路径：`/article/articles/{id}`
- 鉴权：是（仅作者可操作）
- 说明：支持更新草稿、草稿转发布、已发布后再编辑

请求体（同创建）：

```json
{
  "title": "更新后的标题",
  "summary": "更新后的摘要",
  "content": "# 新正文",
  "cover": "https://xxx/new-cover.jpg",
  "categoryId": 2,
  "tagIds": [2, 4],
  "status": 0
}
```

### 5.3 获取标签列表（发布页动态标签）

- 方法：`GET`
- 路径：`/tag`
- 鉴权：否
- 说明：当前 `TagController` 为空，建议补齐用于替换前端硬编码标签

返回示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    { "id": 1, "name": "Vue" },
    { "id": 2, "name": "Spring Boot" }
  ]
}
```

### 5.4 （可选）删除我的文章

- 方法：`DELETE`
- 路径：`/article/articles/{id}`
- 鉴权：是（仅作者）
- 说明：`Mine.vue` 已有“删除”按钮，建议配套接口

### 5.5 （可选）发布页编辑回填专用接口

- 方法：`GET`
- 路径：`/article/articles/{id}/edit`
- 鉴权：是（仅作者）
- 说明：若不新增，可直接复用详情接口；新增可避免暴露不必要字段并处理草稿权限

## 6. 前端调用建议（CreateArticle）

建议在 `src/utils/request.js` 增加：

- `articleApi.createArticle(data)` -> `POST /article/articles/publish`
- `articleApi.updateArticle(id, data)` -> `PUT /article/articles/{id}`
- `tagApi.getTags()` -> `GET /tag`

提交逻辑建议：

- 点击“保存草稿”：
  - `status = 0`
  - 成功后提示“草稿已保存”，可停留当前页
- 点击“发布文章”：
  - `status = 1`
  - 成功后跳转文章详情 `/article/{id}` 或“我的文章”页

## 7. 联调检查清单

- 发布/草稿是否都能入库，并在 `/article/my-articles` 正确区分状态
- 已发布文章是否可在 `/article/articles/{id}` 正常展示
- 封面上传 URL 是否可直接用于文章展示
- 标签关系（`tagIds`）是否正确写入中间表
- 权限是否生效：非作者不可编辑/删除他人文章

---

如需，我可以继续基于该文档直接补齐：

1. `CreateArticle.vue` 的实际接口接入代码  
2. `request.js` 中 `articleApi/tagApi` 新方法  
3. 后端 `Controller + DTO + Service` 的最小可用实现
