# 基于Spring Boot + Vue3 的个人博客系统

## 原始需求

> 基于springboot和vue3写数据库用MySQL，密码200569，一个个人博客

## 目录结构

```text
label-4002/
├── backend/         # Spring Boot 3.3 + Spring Security + JPA + Flyway
├── frontend/        # Vue3 + Vite + TypeScript + Pinia + Element Plus（pnpm）
├── infra/           # Nginx 网关、Docker Compose、数据库初始化脚本
└── docs/adr/        # 架构决策记录
```

## 代码架构

后端（`backend`）采用模块化单体：

- `controller`：按场景拆分公开接口、后台接口、认证接口。
- `service`：封装文章、认证、令牌刷新等业务逻辑。
- `repository`：基于 Spring Data JPA 访问数据库。
- `security`：Session 与 JWT 混合鉴权、统一异常输出。
- `db/migration`：Flyway 版本化变更脚本。

前端（`frontend`）采用 Vue 3 分层：

- `views`：页面级组件（列表、详情、登录、后台）。
- `stores`：Pinia 状态管理（登录态、文章状态）。
- `api`：Axios 请求封装与错误处理。
- `router`：公开路由与后台路由守卫。
- `tests`：Vitest 单测与 Playwright E2E。

网关（`infra/nginx/nginx.conf`）统一入口：

- `/api/*` 转发到后端。
- `/*` 转发到前端。

## 技术细节

- Java 21 + Spring Boot 3.3.x，`Spring Security` 实现 Session/JWT 混合鉴权。
- `Spring Data JPA` + MySQL 8，写操作通过参数绑定避免 SQL 注入。
- `Flyway` 维护数据版本：本地默认 `target=3`，Docker Compose 运行时设置 `target=6`。
- 统一错误体：`{ code, message, traceId }`。
- Session Cookie：`HttpOnly=true`、`SameSite=Lax`，`Secure` 按环境开关。
- JWT：访问令牌短期有效，刷新令牌哈希后持久化到 `refresh_tokens`。
- 日志：JSON 结构化输出，便于链路检索与问题定位。

## API 清单

- `GET /api/v1/posts`
- `GET /api/v1/posts/{id}`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `GET /api/v1/admin/posts/mine`
- `POST /api/v1/admin/posts`
- `PUT /api/v1/admin/posts/{id}`
- `DELETE /api/v1/admin/posts/{id}`
- `POST /api/v1/token/login`
- `POST /api/v1/token/refresh`
- `GET /api/v1/token/secure/me`

OpenAPI：`label-4002/backend/openapi.yaml`

## 本地开发

### 1) 启动数据库

```bash
cd label-4002
docker compose up -d db
```

### 2) 启动后端

```bash
cd label-4002/backend
./mvnw spring-boot:run
```

默认端口：`8081`

### 3) 启动前端（仅 pnpm）

```bash
cd label-4002/frontend
pnpm install
pnpm dev
```

默认端口：`5173`，前端已代理 `/api` 到 `http://127.0.0.1:8081`。

## 一体化部署

```bash
cd label-4002
docker compose up -d --build
```

访问地址：`http://127.0.0.1:8080/`

## 测试

后端：

```bash
cd label-4002/backend
./mvnw test
```

前端单测：

```bash
cd label-4002/frontend
pnpm test:unit
```

全链路 E2E（覆盖页面流程、分支与边界）：

```bash
cd label-4002/frontend
pnpm test:e2e
```

`test:e2e` 会自动拉起后端（`e2e` profile，H2 内存库）和前端开发服务器。

## 默认账号

- 用户名：`admin`
- 密码：`admin123456`
