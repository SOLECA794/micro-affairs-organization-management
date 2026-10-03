# 部署说明（本阶段仅准备模板，不在本阶段执行部署）

> 本目录交付部署所需的配置模板与操作手册，供下一阶段在服务器上执行。
> 部署目标（已确认）：阿里云 ECS `114.55.31.135`，Nginx 8097 独立端口对外（不占 80 根节点），后端仅监听
> `127.0.0.1:8080`；HTTP 演示环境，暂无域名与 HTTPS 证书。

## 1. 目录内容

| 文件 | 用途 |
| --- | --- |
| `nginx/association-system.conf` | Nginx 站点配置模板（静态资源 + SPA 回退 + /api 反代） |
| `systemd/assoc-server.service` | 后端 systemd 服务单元模板 |
| `systemd/assoc-server.env.example` | 环境变量文件模板（数据库、JWT 密钥、二维码基地址） |

## 2. 服务器前置条件

- 阿里云 ECS（114.55.31.135），安全组放行 8097 端口（22 仅限运维来源 IP）；
- JDK 17（`yum install -y java-17-openjdk` 或解压版，记录 `java` 全路径）；
- Nginx（`yum install -y nginx`）；
- MySQL 8（本机或同网段），建库执行 `db/schema.sql`，初始化执行 `db/data.sql`；
- 应用运行账号（`useradd -r -s /sbin/nologin assoc`）。

## 3. 后端部署步骤

1. 上传 `backend/association-system-server/target/association-system-server.jar`
   （本地 `mvn -DskipTests package` 产物，约 52MB）到 `/opt/association-system/`；
2. 复制 `systemd/assoc-server.env.example` 为 `/opt/association-system/assoc-server.env`，
   填写数据库账号密码与 JWT 密钥（生成方式：`openssl rand -base64 48`），`chmod 600`；
3. 复制 `systemd/assoc-server.service` 到 `/etc/systemd/system/`，确认 `ExecStart`
   中 `java` 全路径与服务器一致；
4. `systemctl daemon-reload && systemctl enable --now assoc-server`；
5. 验证：`curl http://127.0.0.1:8080/api/auth/me` 应返回 401 JSON
   （`{"code":40100,...}`），说明服务存活且鉴权生效。

## 4. 前端部署步骤

1. 本地 `frontend/association-system-web` 执行 `npm run build`，产物在 `dist/`；
2. 上传 `dist/` 全部内容到 `/opt/association-system/web/`；
3. 复制 `nginx/association-system.conf` 到 `/etc/nginx/conf.d/`，
   `nginx -t && systemctl reload nginx`。

## 5. 联调验证清单（部署阶段执行）

- 浏览器访问 `http://114.55.31.135:8097/` 出现登录页；
- `admin / admin123` 登录进入管理端，三个角色菜单隔离正确；
- 学生端报名 → 社团端发布/开启签到 → 学生扫码（手机与 PC 同网段）→ 统计与导出；
- 二维码基地址在 `assoc-server.env` 中配置为 `http://114.55.31.135:8097`，
  手机扫码可直达签到落地页。

## 6. 回滚

- 后端：`systemctl stop assoc-server`，替换 jar 后 `start`；
- 前端：保留上一版本 `web/` 目录备份，覆盖回退；
- 数据库：本阶段无增量迁移；如需回滚数据，使用部署前 `mysqldump` 快照。

## 7. 线上口令已轮换（2026-10-04）

**线上环境的 admin、mgr001、mgr002 口令已在安全整改中轮换为强随机密码（各 ≥16 位）**，
密文已直接更新至线上 MySQL，新口令仅保存在服务器 `/root/assoc-credentials`（权限 600）。

`db/data.sql` 中的初始口令（admin123 等）**不再适用于线上环境**，仅适用于
按第 2 节流程全新初始化的本地/新环境。学生演示账号 stu001~stu005 仍为 stu123456
（仅报名权限，便于演示）。首次在任何可公网访问的环境初始化后，必须立即参照本轮换流程
更换管理员与负责人口令。
