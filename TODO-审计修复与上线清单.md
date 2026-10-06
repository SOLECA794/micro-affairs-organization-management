# 审计修复与上线待办清单

> 生成：2026-10-04 ｜ 依据：代码审计报告（演示 8/10，真实工具 4/10）
> 代码层修复已全部提交并推送（`d4f9738`→`6bb0336` 共 6 个 commit）；**服务器尚未重新部署**。
> 线上地址：http://114.55.31.135:8097（nginx 8097 → 127.0.0.1:8080，MySQL 仅本机）

## 已完成（代码已入库，勿重复做）

- [x] 递补超卖缺陷：取消时名额改为"转移"语义（有候补递补则不释放名额）`SignupService`
- [x] 4 个负责人读接口横向越权：signups / 签到名单 / 未签到名单 / 统计，全部补归属校验（40300）
- [x] 停用用户 token 立即失效：拦截器解析后实时查库校验 status/deleted（`JwtInterceptor:75`）
- [x] 匿名活动列表 status 白名单（`?status=DRAFT` 不再泄露草稿）
- [x] 操作日志 IP 异步 ThreadLocal 修复（请求线程内捕获）
- [x] 负责人/管理员通知接口放开 + 前端「我的消息」入口
- [x] 管理员创建用户 + 重置密码接口（BCrypt、一次性明文返回、写日志）+ 用户管理页弹窗
- [x] 公开无 PII 选项端点（/api/options/associations、/api/options/categories）+ JWT 白名单
- [x] 审核页详情弹窗（消除盲审、修复死链）、分类删除文案对齐、社团绑定负责人防重复
- [x] 学生端活动列表补社团/分类/时间范围筛选
- [x] 本地构建验证：后端 jar 已重新打包（target/ 时间戳晚于修复提交）、前端 npm build 通过
- [x] 文档同步（04 接口入册、deploy README 口令说明）

## 待办 A：上线三件事（下一轮工作，预计半天）

1. **重新部署后端**：分块上传改动源码到 `/opt/assoc/server`（工具包 §3.4）→ `/opt/assoc/maven/bin/mvn -DskipTests package`（MAVEN_OPTS=-Xmx512m，setsid nohup 防云助手回收）→ 替换 `/opt/assoc/app.jar` → `systemctl restart assoc-server` → 确认 127.0.0.1:8080 健康
2. **重新部署前端**：本地 `npm run build` → dist 打包分块上传 → 替换 `/opt/assoc/web`
3. **线上口令轮换**：admin / mgr001 / mgr002 换 ≥16 位强随机口令（`tools/BcryptHashGenerator.java` 生成密文 → 服务器 UPDATE sys_user → 新口令追加到 `/root/assoc-credentials`，600）。当前线上仍是 admin/admin123，公网可猜，**优先级最高**

## 待办 B：线上回归验证（部署后必做，全部从公网对 8097 执行）

- [ ] 超卖回归：capacity=1 活动 → 报名→候补→取消（递补后 enrolled_count 仍=1）→ 新报名必须进 WAITING
- [ ] 越权回归：mgr002 请求 mgr001 活动的 signups/attendance/unsigned/statistics → 全 40300
- [ ] 停用踢人：建测试用户→登录→停用→旧 token 请求 → 40100
- [ ] 账号闭环：admin 建用户→一次性密码登录→重置密码→新旧密码行为正确（测完停用）
- [ ] 匿名 `?status=DRAFT` 无数据；/api/options/* 匿名 200 且仅 id+name
- [ ] mgr001 能读自己的通知；触发操作后 /api/admin/logs 的 ip 非空
- [ ] admin/mgr 旧口令登录失败、新口令成功
- [ ] `free -m` 与 `ss -tlnp` 对比，同机其他业务原样（8095/8096/8088 等勿动）

## 待办 C：真实可用还差的远期项（审计遗留，按需排期）

- [ ] HTTPS（免费证书 + nginx 443 + 更新 QRCODE/FRONT_BASE_URL）——真实使用前必须
- [ ] 封面/头像本地上传（multipart 配置已有但无接口，天级）
- [ ] 自动化测试（src/test 为空；至少给报名/递补/签到写集成测试）
- [ ] 登录失败限流/锁定；导出名单数据最小化（是否含已取消人员）
- [ ] 运维：AccessKey 轮换（工具包既有待办）、3389 规则收紧、nginx 中 teacher-web 残留清理

## 遗留决策

- `docs-proposals/`（NestJS+PostgreSQL 提案快照）留档中，删留待项目负责人明确
- 文档内【待确认】：完成时间、演示学生姓名清单
