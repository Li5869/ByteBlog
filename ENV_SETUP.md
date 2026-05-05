# 环境变量配置指南

本文档详细说明 ByteBlog 项目在开源部署时需要配置的环境变量。所有敏感信息已从代码中移除，必须通过环境变量注入。

---

## 一、快速开始

### 1.1 复制模板文件

**Java 后端项目：**
```bash
cd project-backen
cp .env.example .env
```

**Python AI Agent 项目：**
```bash
cd project-ai-agent
cp .env.example .env
```

### 1.2 填写环境变量

编辑 `.env` 文件，填写所有必需的配置项。

---

## 二、Java 后端环境变量

### 2.1 数据库配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `DB_HOST` | PostgreSQL 数据库主机地址 | `localhost` | ✅ |
| `DB_PORT` | PostgreSQL 数据库端口 | `5432` | ✅ |
| `DB_NAME` | 数据库名称 | `person_blog` | ✅ |
| `DB_USERNAME` | 数据库用户名 | `postgres` | ✅ |
| `DB_PASSWORD` | 数据库密码 | - | ✅ |

### 2.2 Redis 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `REDIS_HOST` | Redis 主机地址 | `localhost` | ✅ |
| `REDIS_PORT` | Redis 端口 | `6379` | ✅ |
| `REDIS_PASSWORD` | Redis 密码 | - | ✅ |
| `REDIS_DATABASE` | Redis 数据库索引 | `2` | ✅ |

### 2.3 RabbitMQ 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `RABBITMQ_HOST` | RabbitMQ 主机地址 | `localhost` | ✅ |
| `RABBITMQ_PORT` | RabbitMQ 端口 | `5672` | ✅ |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | `guest` | ✅ |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | `guest` | ✅ |

### 2.4 Elasticsearch 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `ES_URIS` | Elasticsearch 连接地址 | `http://localhost:9200` | ✅ |

### 2.5 JWT 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `JWT_SECRET` | JWT 签名密钥 | - | ✅ |

**安全要求：**
- 长度至少 64 个字符
- 使用强随机字符串生成
- 生产环境必须与开发/测试环境不同

**生成示例：**
```bash
# Linux/macOS
openssl rand -base64 64

# 或使用 Python
python3 -c "import secrets; print(secrets.token_urlsafe(64))"
```

### 2.6 XXL-Job 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `XXL_JOB_ADMIN_ADDRESSES` | XXL-Job 调度中心地址 | `http://localhost:8080/xxl-job-admin` | ⚪ |
| `XXL_JOB_ACCESS_TOKEN` | XXL-Job 访问令牌 | - | ⚪ |
| `XXL_JOB_EXECUTOR_IP` | 执行器 IP（公网 IP） | - | ⚪ |
| `XXL_JOB_EXECUTOR_PORT` | 执行器端口 | `9999` | ⚪ |

> ⚪ 表示可选，如不使用 XXL-Job 可不配置

### 2.7 阿里云 OSS 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `OSS_ACCESS_KEY_ID` | 阿里云 AccessKey ID | - | ⚪ |
| `OSS_ACCESS_KEY_SECRET` | 阿里云 AccessKey Secret | - | ⚪ |
| `ALIYUN_OSS_ENDPOINT` | OSS 地域节点 | `oss-cn-beijing.aliyuncs.com` | ⚪ |
| `ALIYUN_OSS_BUCKET_NAME` | OSS 存储桶名称 | - | ⚪ |

> ⚪ 表示可选，如不使用 OSS 可不配置

### 2.8 OpenAI / Spring AI 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `OPENAI_BASE_URL` | OpenAI API 基础 URL | `https://api.openai.com` | ✅ |
| `OPENAI_API_KEY` | OpenAI API 密钥 | - | ✅ |
| `OPENAI_MODEL` | 默认使用的模型 | `gpt-4o-mini` | ✅ |

### 2.9 Python AI 服务配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `PYTHON_SERVICE_URL` | Python AI Agent 服务地址 | `http://127.0.0.1:8000` | ✅ |

### 2.10 API Key 认证配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `API_UNIVERSAL_KEY` | 内部服务间通信的万能密钥 | - | ✅ |

**安全要求：**
- 使用强随机字符串
- 生产环境必须与开发环境不同
- 定期更换

---

## 三、Python AI Agent 环境变量

### 3.1 DeepSeek 配置（推荐）

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `OPENAI_API_KEY_DEEPSEEK` | DeepSeek API 密钥 | - | ✅ |
| `OPENAI_BASE_URL_DEEPSEEK` | DeepSeek API 地址 | `https://api.deepseek.com` | ✅ |
| `MODEL_NAME_DEEPSEEK` | DeepSeek 模型名称 | `deepseek-chat` | ✅ |

### 3.2 OpenAI 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `OPENAI_API_KEY` | OpenAI API 密钥 | - | ⚪ |
| `OPENAI_BASE_URL` | OpenAI API 地址 | `https://api.openai.com/v1` | ⚪ |
| `EMBEDDING_MODEL` | Embedding 模型 | `text-embedding-3-small` | ⚪ |
| `MODEL_NAME` | OpenAI 模型名称 | `gpt-4o-mini` | ⚪ |

### 3.3 搜索引擎配置（Tavily）

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `TAVILY_API_KEY` | Tavily API 密钥 | - | ⚪ |

> ⚪ 表示可选，如不使用外部搜索可不配置

### 3.4 后端 API 通信配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `BACKEND_API_KEY` | 与 Java 后端通信的密钥 | - | ✅ |

> ⚠️ 必须与 Java 后端的 `API_UNIVERSAL_KEY` 保持一致

### 3.5 数据库配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `DATABASE_URL` | PostgreSQL 连接字符串 | `postgresql://postgres:postgres@localhost:5432/person_blog` | ✅ |
| `REDIS_URL` | Redis 连接字符串 | `redis://localhost:6379/2` | ✅ |
| `ES_HOST` | Elasticsearch 地址 | `http://localhost:9200` | ✅ |

### 3.6 博客后端 API 配置

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `BACKEND_API_BASE` | Java 后端 API 地址 | `http://localhost:8080/api` | ✅ |

---

## 四、部署方式

### 4.1 Docker Compose 部署

创建 `docker-compose.yml` 时使用 `environment` 注入：

```yaml
services:
  backend:
    image: byteblog-backend:latest
    environment:
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=person_blog
      - DB_USERNAME=postgres
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      # ... 其他配置
```

### 4.2 Kubernetes 部署

使用 `ConfigMap` 和 `Secret` 管理：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: byteblog-secrets
stringData:
  DB_PASSWORD: "your-db-password"
  REDIS_PASSWORD: "your-redis-password"
  JWT_SECRET: "your-jwt-secret"
  OPENAI_API_KEY: "your-openai-api-key"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: byteblog-config
data:
  DB_HOST: "postgres-service"
  DB_PORT: "5432"
  REDIS_HOST: "redis-service"
```

### 4.3 传统部署

**方式一：系统环境变量**
```bash
# Linux/macOS
export DB_HOST=localhost
export DB_PASSWORD=your-password
export JWT_SECRET=your-jwt-secret
java -jar blog-application.jar
```

**方式二：启动参数**
```bash
java -jar blog-application.jar \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/person_blog \
  --spring.datasource.password=your-password \
  --jwt.secret=your-jwt-secret
```

**方式三：.env 文件（开发环境）**
```bash
# 复制模板
cp .env.example .env
# 编辑 .env 文件填写配置
# 启动应用（需配合 Spring Boot 的环境变量支持）
```

---

## 五、安全建议

### 5.1 密钥管理

1. **永远不要提交 `.env` 文件到 Git**
   - `.gitignore` 已配置忽略 `.env` 文件
   - 只提交 `.env.example` 模板文件

2. **使用强随机密钥**
   ```bash
   # 生成 64 字符随机密钥
   openssl rand -base64 48
   ```

3. **不同环境使用不同密钥**
   - 开发环境
   - 测试环境
   - 生产环境

### 5.2 密钥轮换

建议定期轮换以下密钥：
- JWT Secret（每 90 天）
- API Keys（每 180 天）
- 数据库密码（每 180 天）

### 5.3 访问控制

- 限制数据库访问 IP
- Redis 启用密码认证
- RabbitMQ 使用非默认用户名密码
- API Key 不要硬编码在前端代码中

---

## 六、故障排查

### 6.1 常见错误

| 错误信息 | 可能原因 | 解决方案 |
|---------|---------|---------|
| `Failed to configure a DataSource` | 数据库连接失败 | 检查 `DB_*` 环境变量 |
| `Unable to connect to Redis` | Redis 连接失败 | 检查 `REDIS_*` 环境变量 |
| `JWT signature does not match` | JWT 密钥不匹配 | 确保 `JWT_SECRET` 一致 |
| `API Key authentication failed` | API Key 不匹配 | 确保 `API_UNIVERSAL_KEY` 一致 |

### 6.2 验证配置

**Java 后端：**
```bash
# 查看当前配置
curl http://localhost:8080/api/actuator/env
```

**Python AI Agent：**
```bash
# 打印当前配置（调试模式）
python -c "from config.settings import get_settings; print(get_settings().model_dump())"
```

---

## 七、配置文件清单

| 文件 | 位置 | 说明 |
|------|------|------|
| `application.yml` | `project-backen/blog-application/src/main/resources/` | 主配置文件 |
| `application-dev.yml` | `project-backen/blog-application/src/main/resources/` | 开发环境配置 |
| `application-prod.yml` | `project-backen/blog-application/src/main/resources/` | 生产环境配置 |
| `application-ai.yml` | `project-backen/blog-application/src/main/resources/` | AI 模块配置 |
| `.env.example` | `project-backen/` | 环境变量模板 |
| `settings.py` | `project-ai-agent/config/` | Python 配置管理 |
| `.env.example` | `project-ai-agent/` | Python 环境变量模板 |

---

## 八、联系与支持

如有问题，请提交 Issue 或 Pull Request。
