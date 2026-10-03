# docs-proposals · 并发修改快照（NestJS/PostgreSQL 技术栈提案）

本目录保存的是 2026-10-04 00:05-00:06 期间工作区中出现的 4 份文档未提交修改快照
（01/02/03/05），其内容将技术栈整体调整为：

- 前端：React 18 + TypeScript + Tailwind CSS
- 后端：Node.js 20 + NestJS 10 + Prisma + PostgreSQL 16 + Redis
- 认证：jose + argon2
- 部署：Docker Compose / PM2

## 为什么保存快照

本次交付简报明确规定「技术栈最终决策（已定）：Java 17 + Spring Boot 3.3.x +
MyBatis-Plus + MySQL 8 后端、React 18 + JavaScript + Ant Design 5 前端，
不得更改」，且全部后端代码、`db/schema.sql`（MySQL 方言）均按该基线完成并通过
构建验证。上述文档修改与代码基线冲突，且为未提交的外部并发修改、来源未明。

为不销毁任何工作成果，快照留存于此，供团队评审：

- 若确认切换 NestJS/PostgreSQL 方案：需重新开发后端（当前 Spring Boot 实现不可复用），
  并重写数据库脚本与本文档集，属于新立项级别的变更；
- 若维持现行基线：直接删除本目录即可。

## 交付版文档

仓库中的 01/02/03/04/05 与 README.md 已按交付简报完成基线修订
（前端 Vue → React 的技术栈变更记录），与代码、数据库脚本保持一致。
