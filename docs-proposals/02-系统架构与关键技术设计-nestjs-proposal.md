# 02 · 系统架构与关键技术设计

## 1. 总体架构

系统采用浏览器访问、前后端分离架构：

```
浏览器（学生端 / 社团管理端 / 系统管理端，React SPA，响应式）
        │  HTTPS / HTTP  RESTful JSON
        ▼
Nginx（前端静态资源托管 + /api 反向代理）
        │
        ▼
NestJS 应用（Node.js 20 + TypeScript）
  ├── 安全层：JWT 认证（Guard）、角色鉴权、参数校验（Pipe）
  ├── 业务模块：auth / users / associations / activities / signups / signins / statistics / notifications
  └── 数据层：Prisma Client 访问 PostgreSQL；Redis 承担限流与短时效数据
        │
        ▼
PostgreSQL 16（业务数据 + 事务 + 唯一约束 + 条件更新）
Redis（限流计数、签到码短时效缓存、热点数据缓存）
```

- 前端只做页面展示、表单校验与状态反馈，不持有业务规则；
- 后端统一处理角色权限、活动状态、报名名额、候补递补与签到有效性；
- 涉及名额变化的操作使用事务与条件更新，避免并发下超额报名。

## 2. 技术栈清单

| 层次 | 技术 | 选型理由 |
| --- | --- | --- |
| 前端 | React 18、TypeScript、Vite、Ant Design、React Router、Zustand、Axios、Tailwind CSS | React 生态最活跃、组件库与资料最丰富；TS 类型安全；Ant Design 适合管理端页面，配合响应式适配学生端 |
| 后端 | Node.js 20 LTS、TypeScript、NestJS 10 | Node 生态最主流的工程化框架；模块化结构清晰，适合 4 人分工与答辩展示；全 TS 与前端统一 |
| ORM | Prisma | 类型安全、自带 schema 与迁移管理（migrate + seed），数据库结构与代码可追溯 |
| 认证 | JWT（jose）、argon2 | JWT 无状态认证仍是主流；argon2 为现代推荐密码哈希算法 |
| 数据库 | PostgreSQL 16 | 功能最强的开源关系库：约束完整、事务可靠、原生 UTF-8；支持扩展（pgvector 等，为后续能力留路） |
| 缓存 | Redis（ioredis） | 接口限流、签到码短时效、热点活动缓存 |
| 构建 | pnpm | 安装快、依赖管理严格 |
| 部署 | Docker Compose（postgres + redis + api + nginx）或 PM2 + Nginx | 环境可复现，便于团队与演示 |
| 工具 | VS Code、Git、Apifox、DBeaver/pgAdmin、draw.io | 见基础计划 3.2 |

## 3. 后端工程结构与分层

建议工程名：association-system-server，NestJS 标准结构。

```
src
├── main.ts                  # 入口：启动、全局前缀 /api、Swagger
├── app.module.ts            # 根模块
├── common                   # 统一响应体、错误码、全局异常过滤器、日志拦截器
├── config                   # 环境配置（ConfigModule）、Redis 连接
├── auth                     # 登录、JWT 策略、Guard、角色装饰器
└── modules
    ├── users                # 用户管理
    ├── associations         # 社团管理
    ├── activities           # 活动管理与审核
    ├── signups              # 报名、取消、候补递补
    ├── signins              # 签到码、扫码签到、补签
    ├── statistics           # 统计与导出
    └── notifications        # 站内通知
```

分层职责：Controller 只做参数接收与返回；Service 承载业务规则与事务（Prisma 事务）；Prisma 的 schema 与迁移位于 `prisma/` 目录。禁止在 Controller 中直接写数据访问。

## 4. 前端工程结构

建议工程名：association-system-web，Vite + React + TypeScript。

```
src
├── api                # 按模块封装的请求方法（auth.ts、activity.ts、signup.ts、signin.ts、admin.ts）
├── router             # 路由定义 + 守卫（Token 校验、角色校验）
├── stores             # Zustand：user（Token、用户信息、角色）、menu
├── pages
│   ├── student        # 学生端：活动列表、活动详情、我的报名、个人中心、我的通知
│   ├── manager        # 社团端：工作台、活动管理、报名名单、签到管理、统计
│   └── admin          # 管理端：用户、社团、分类、审核、日志
├── components         # 通用组件：分页表格、状态标签、二维码展示、文件上传
├── utils              # axios 封装（拦截器、统一错误提示）、时间/状态格式化
└── styles             # 全局样式、Tailwind 配置、主题
```

Axios 封装要求：请求头自动携带 `Authorization: Bearer <token>`；响应统一解包 `{code,message,data}`；401 时清除登录态并跳转登录页；业务错误码统一提示。

## 5. 关键设计

### 5.1 认证与权限

- 登录：账号密码 → 校验 argon2 → 签发 JWT（负载含 userId、role），返回 Token 与用户信息。
- 请求鉴权：NestJS Guard 解析 Authorization 头，校验签名与有效期，写入当前用户上下文（`@CurrentUser()`）；未携带或无效返回 401。
- 角色控制：自定义 `@Roles('MANAGER')` 装饰器 + RolesGuard，不匹配返回 403。
- 前端：路由守卫校验 Token；菜单按角色过滤；按钮级权限按角色判断。
- 密码修改后失效策略：本期采用"修改密码即退出重新登录"的简单方案，避免引入 Token 黑名单。

### 5.2 活动状态机

状态：`DRAFT → PENDING → APPROVED → PUBLISHED → ENDED → ARCHIVED`；`PENDING → REJECTED → DRAFT`；`PUBLISHED → CANCELLED`。

流转规则：
- 负责人创建后为 DRAFT，可编辑；提交审核 → PENDING；
- 管理员审核通过 → APPROVED，负责人发布 → PUBLISHED；
- 驳回 → REJECTED（携带意见），负责人可修改后重新提交；
- 报名截止或开始后不可再报名（查询与写入双重判断）；
- 结束时间到达 → 定时任务（Nest 定时任务或发布时预约）置为 ENDED；负责人或管理员归档 → ARCHIVED；
- 已发布可取消 → CANCELLED，通知已报名学生。

状态统一由后端维护与校验，前端仅展示。时间判断以服务器时间为准，不允许前端时间参与业务判定。

### 5.3 报名名额并发控制

目标：并发下不超额报名、不重复报名。

方案（三层保障）：
1. 唯一约束：`signup` 表 `UNIQUE(activity_id, user_id)`，从数据库层杜绝重复报名；
2. 条件更新占位：报名前执行
   `UPDATE activity SET enrolled_count = enrolled_count + 1 WHERE id = $1 AND status = 'PUBLISHED' AND enrolled_count < capacity`
   受影响行数 = 1 才继续插入报名记录，否则返回"名额已满"（进入候补分支）；该 UPDATE 与 INSERT 必须在同一事务（Prisma `$transaction`）；
3. 状态与时间校验：报名事务内再次校验活动状态为 PUBLISHED、当前时间未过 signup_deadline。

取消报名：事务内执行 `UPDATE activity SET enrolled_count = enrolled_count - 1 WHERE id = $1 AND enrolled_count > 0`，再更新报名记录状态为 CANCELLED，最后触发递补。

### 5.4 候补与递补

- 满员报名：插入状态 WAITING 的记录，`queue_order = 当前该活动候补最大序号 + 1`。
- 递补时机：仅在取消报名的事务内触发，避免并发重复递补。
- 递补逻辑（同一事务）：按 `queue_order ASC` 取该活动 WAITING 第一条 → 状态更新为 ACTIVE → 发站内通知。若被递补者已取消或已报名其他限制，本期不处理，按顺序递补下一位。
- 递补与名额释放一致：先释放名额，再递补，保证名额不超卖。

### 5.5 限时二维码签到

- 开启签到：负责人对 PUBLISHED/ENDED 状态活动调用"开启签到"接口，后端生成签到码记录：
  `token = randomUUID()`，`expires_at = now + 5 分钟`，同一活动同一时间只允许一个有效签到码（新开启则作废旧码）。
- 二维码内容：前端将签到接口地址与 token 编码为二维码展示（如 `https://host/api/signin/qrcode?token=xxx`）。
- 扫码签到：学生提交 `{activityId, token}` → 后端校验：
  1. 签到码存在且未过期、状态有效（Redis 辅助快速校验，DB 为最终依据）；
  2. 活动状态允许签到；
  3. 该学生报名状态为 ACTIVE（未报名不可签到）；
  4. 未重复签到（`attendance` 表 `UNIQUE(activity_id, user_id)`）。
  全部通过 → 写入签到记录。
- 过期/重复/未报名均返回明确业务错误码并记录异常日志。
- 人工补签：负责人选择名单内 ACTIVE 且未签到成员，执行补签，`sign_type = MANUAL`、记录 `operator_id` 与时间。

### 5.6 统计与导出

- 统计口径：报名人数 = ACTIVE 报名数；签到人数 = attendance 数；缺席 = ACTIVE 且未签到；到场率 = 签到人数 / 报名人数 × 100%。
- 统计接口返回分组数据（按活动、按社团），页面用表格与图表展示。
- 导出：使用 exceljs 生成报名名单/签到名单，字段与页面列表一致；文件名含活动名与导出时间。

### 5.7 站内通知

通知触发节点（写入 notification 表，学生端个人中心"我的消息"可读）：

| 节点 | 通知对象 | 内容 |
| --- | --- | --- |
| 活动审核通过/驳回 | 负责人 | 审核结果与意见 |
| 报名成功 | 学生 | 活动信息与报名状态 |
| 进入候补 | 学生 | 当前候补序号 |
| 递补成功 | 学生 | 已递补为报名成功 |
| 活动取消 | 已报名学生 | 取消说明 |
| 开启签到 | 已报名学生 | 签到入口提醒 |

### 5.8 操作日志

记录范围：登录、报名、取消报名、递补、开启签到、扫码签到、补签、审核、活动发布/取消/归档。字段：操作人、模块、动作、详情、IP、时间。管理端日志页可查询（日志写入用全局拦截器/装饰器统一处理）。

## 6. 部署架构

推荐 Docker Compose（演示环境一键拉起）：

```
docker-compose.yml
├── postgres   # PostgreSQL 16，持久化卷
├── redis      # Redis 7
├── api        # NestJS 构建产物，暴露 3000 端口
└── nginx      # 前端静态资源 + /api 反代到 api:3000
```

不使用 Docker 时的等价方案：`pnpm build` 产出后端 dist 与前端静态目录；PM2 守护 Node 进程；Nginx 托管前端并把 `/api` 代理到 `127.0.0.1:3000`。

部署要点：
- 前端构建产物放置 Nginx 站点目录；`/api` 前缀代理，避免跨域；
- 后端启用 CORS 白名单（开发环境放开，生产收紧）；
- 数据库连接、JWT 密钥、签到码有效期等配置走环境变量（`.env` + `.env.production`），密钥不入库不入仓库；
- 【待确认】演示部署的服务器地址、域名与 HTTPS 证书。

## 7. 关键风险与对策

| 风险 | 对策 |
| --- | --- |
| 并发超额报名/重复报名 | 唯一约束 + 条件更新 + 事务（见 5.3） |
| 并发重复递补 | 递补仅在取消事务内执行；后续可加行锁复核 |
| 二维码被转发/过期 | 限时 5 分钟 + 服务端校验有效期 + 一次性有效 + 异常日志 |
| 服务器与客户端时间不一致 | 业务时间判定统一取服务器时间 |
| 跨域与安全配置遗漏 | 开发环境 CORS 白名单；生产走 Nginx 同域代理 |
| 数据库结构与代码不同步 | Prisma schema 为唯一来源，迁移纳入版本管理（见 03 文档） |