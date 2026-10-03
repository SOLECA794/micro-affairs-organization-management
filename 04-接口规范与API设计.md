# 04 · 接口规范与 API 设计

## 1. 通用约定

- 基础路径：`/api`；RESTful 风格；请求与响应均为 JSON（`application/json`；charset=utf-8）；
- 认证：请求头 `Authorization: Bearer <token>`；未携带或失效返回 401；
- 时间格式：`yyyy-MM-dd HH:mm:ss`；
- 分页：查询列表统一使用 `page`（从 1 起）与 `size`（默认 10，最大 100）；
- 幂等与安全：写操作校验角色与状态；二维码签到接口限流（防刷）；
- 接口文档维护在 Apifox，字段变更需前后端同步更新。

## 2. 统一响应结构

成功：

```json
{ "code": 0, "message": "ok", "data": { } }
```

失败：

```json
{ "code": 40001, "message": "报名已截止", "data": null }
```

分页数据：

```json
{ "code": 0, "message": "ok", "data": { "list": [], "total": 0, "page": 1, "size": 10 } }
```

## 3. 错误码约定

| 区间 | 含义 |
| --- | --- |
| 0 | 成功 |
| 40001–40099 | 参数校验错误（业务参数） |
| 40100 | 未登录或 Token 失效 |
| 40300 | 无权限（角色越权） |
| 40400 | 资源不存在 |
| 40901 | 重复操作（重复报名/重复签到） |
| 40902 | 名额已满 |
| 40903 | 状态不允许（活动状态/时间不满足） |
| 50000 | 系统异常（兜底） |

前端按 code 统一提示 message；401 额外触发清除登录态并跳转登录页。

## 4. API 清单

角色标记：公开 / 学生 / 负责人(MANAGER) / 管理员(ADMIN)。

### 4.1 认证 auth

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 公开 | 登录，返回 token 与用户信息（含角色） |
| POST | /api/auth/logout | 全部 | 退出（前端清除 Token） |
| POST | /api/auth/password | 全部 | 修改本人密码（原密码、新密码） |
| GET | /api/auth/me | 全部 | 当前登录用户信息 |
| PUT | /api/auth/profile | 全部 | 维护本人资料（姓名、手机号、头像；实现期扩展，对应 01 文档个人资料需求） |

### 4.2 学生端 student

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| GET | /api/activities | 学生/公开 | 活动列表（按社团、时间、状态、关键词，分页；status 仅接受 PUBLISHED/ENDED/ARCHIVED/CANCELLED，白名单外视为未传并默认 PUBLISHED——防草稿/待审核枚举，安全修复） |
| GET | /api/activities/{id} | 学生 | 活动详情（含剩余名额、报名状态） |
| POST | /api/activities/{id}/signup | 学生 | 报名（成功或进入候补） |
| POST | /api/activities/{id}/cancel | 学生 | 取消报名（截止前；有候补时名额转移给候补者，enrolled_count 不变） |
| GET | /api/me/signups | 学生 | 我的报名（状态：已报名/候补/已取消） |
| POST | /api/signin/qrcode | 学生 | 扫码签到（activityId、token） |
| GET | /api/me/notifications | 全部登录角色 | 我的通知（分页，含未读标记；实现期扩展：由仅学生放开为任意登录角色） |
| GET | /api/signin/qrcode | 公开 | 扫码落地页（校验 token 后 302 跳转前端活动页携带签到参数，见 §5.3；浏览器跳转接口） |
| POST | /api/me/notifications/{id}/read | 全部登录角色 | 标记通知已读（仅本人通知） |
| GET | /api/options/associations | 公开 | 启用社团下拉选项 [{id, name}]（实现期扩展，无 PII） |
| GET | /api/options/categories | 公开 | 活动分类下拉选项 [{id, name}]（实现期扩展） |

### 4.3 社团端 manager（负责人）

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| GET | /api/manager/activities | MANAGER | 本社团活动列表（含草稿/待审核） |
| GET | /api/manager/activities/{id} | MANAGER | 本社团活动详情（编辑页回填；实现期扩展） |
| POST | /api/manager/activities | MANAGER | 创建活动（草稿） |
| PUT | /api/manager/activities/{id} | MANAGER | 修改草稿/被驳回活动 |
| POST | /api/manager/activities/{id}/submit | MANAGER | 提交审核 |
| POST | /api/manager/activities/{id}/publish | MANAGER | 发布（需审核通过） |
| POST | /api/manager/activities/{id}/cancel | MANAGER | 取消活动（已发布） |
| POST | /api/manager/activities/{id}/archive | MANAGER | 归档（已结束） |
| GET | /api/manager/activities/{id}/signups | MANAGER | 报名名单（按状态筛选、分页） |
| POST | /api/manager/activities/{id}/signin/open | MANAGER | 开启签到，返回签到码与二维码内容 |
| POST | /api/manager/activities/{id}/signin/manual | MANAGER | 人工补签（userId） |
| GET | /api/manager/activities/{id}/statistics | MANAGER | 统计（报名/签到/缺席/到场率） |
| GET | /api/manager/activities/{id}/attendance | MANAGER | 签到名单列表（实现期扩展，供签到管理页展示） |
| GET | /api/manager/activities/{id}/unsigned | MANAGER | 未签到的报名成功成员（实现期扩展，补签候选） |
| GET | /api/manager/activities/{id}/export/signups | MANAGER | 导出报名名单（Excel） |
| GET | /api/manager/activities/{id}/export/attendance | MANAGER | 导出签到名单（Excel） |
| GET | /api/manager/association | MANAGER | 本社团资料 |
| GET | /api/manager/statistics | MANAGER | 工作台统计概览（活动/报名/签到总数与最近活动；实现期扩展） |
| PUT | /api/manager/association | MANAGER | 维护本社团资料 |

### 4.4 管理端 admin

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| GET | /api/admin/users | ADMIN | 用户列表（分页、筛选） |
| POST | /api/admin/users | ADMIN | 创建用户（STUDENT/MANAGER；初始密码留空由服务端生成随机 8 位，响应明文返回一次；实现期扩展） |
| PUT | /api/admin/users/{id} | ADMIN | 启用/停用用户 |
| PUT | /api/admin/users/{id}/password | ADMIN | 重置用户密码（可指定或服务端生成，响应明文返回一次，写操作日志；实现期扩展） |
| GET | /api/admin/associations | ADMIN | 社团列表 |
| POST | /api/admin/associations | ADMIN | 创建社团 |
| PUT | /api/admin/associations/{id} | ADMIN | 维护社团（含负责人、状态） |
| GET | /api/admin/categories | ADMIN | 分类列表 |
| POST | /api/admin/categories | ADMIN | 新增分类 |
| PUT | /api/admin/categories/{id} | ADMIN | 修改分类 |
| DELETE | /api/admin/categories/{id} | ADMIN | 删除分类 |
| GET | /api/admin/audits | ADMIN | 待审核活动列表 |
| POST | /api/admin/audits/{id}/approve | ADMIN | 审核通过 |
| POST | /api/admin/audits/{id}/reject | ADMIN | 驳回（必填意见） |
| GET | /api/admin/logs | ADMIN | 操作日志查询（分页、筛选） |

## 5. 关键接口字段说明

### 5.1 活动列表（GET /api/activities）

请求参数：`keyword`、`associationId`、`categoryId`、`status`、`startTimeBegin`、`startTimeEnd`、`page`、`size`。

响应 data.list 每项：

```json
{
  "id": 1,
  "title": "社团招新见面会",
  "associationName": "XX 社团",
  "categoryName": "文体活动",
  "location": "图书馆报告厅",
  "startTime": "2026-10-20 19:00:00",
  "endTime": "2026-10-20 21:00:00",
  "signupDeadline": "2026-10-19 23:59:59",
  "capacity": 100,
  "enrolledCount": 78,
  "status": "PUBLISHED"
}
```

### 5.2 报名（POST /api/activities/{id}/signup）

响应 data：

```json
{ "signupId": 10, "result": "ACTIVE" }
```

`result` 取值：`ACTIVE`（报名成功）、`WAITING`（进入候补，附 `queueOrder`）。失败按错误码返回（40902 满员、40903 状态不允许、40901 重复报名）。

### 5.3 开启签到（POST /api/manager/activities/{id}/signin/open）

响应 data：

```json
{ "token": "uuid-string", "expiresAt": "2026-10-20 18:55:00", "qrcodeUrl": "https://host/api/signin/qrcode?token=uuid-string" }
```

前端将 `qrcodeUrl` 编码为二维码展示。

### 5.4 扫码签到（POST /api/signin/qrcode）

请求：`{ "activityId": 1, "token": "uuid-string" }`。响应 data 为签到记录；40901 表示重复签到，40001 表示过期或无效签到码。

### 5.5 创建用户 / 重置密码（实现期扩展）

- `POST /api/admin/users`：请求 `{username, realName, role: STUDENT|MANAGER, phone?, password?}`；
  响应 `data: {id, username, realName, role, initialPassword}`。`initialPassword` 为一次性明文
  （客户端未传时由服务端生成随机 8 位），仅在该响应中出现，不落库明文。
- `PUT /api/admin/users/{id}/password`：请求 `{password?}`，留空由服务端生成；响应同上。
  操作写 operation_log（admin 模块，"创建用户"/"重置密码"）。
- `GET /api/options/associations`、`GET /api/options/categories`：公开匿名，响应
  `data: [{id, name}]`，仅暴露 id 与名称，不含负责人、联系方式等 PII。

## 6. 开发期建议

- 后端先行按本文档实现并维护 Apifox；前端按 Apifox 联调；
- 字段增删改必须同步更新本文档与 Apifox；涉及数据库变更先走全组评审；
- 二维码内容在生产部署后回填真实域名（见 02 文档部署章节）。
