-- =============================================================
-- 社团活动报名与签到系统 · 初始化数据（幂等：INSERT IGNORE）
-- 依据：03-数据库设计.md §6
-- 密码说明：全部为真实 BCrypt 密文（spring-security-crypto 6.3.5 BCrypt，
--           成本因子 10），由 tools/BcryptHashGenerator.java 生成，
--           每条均通过 BCrypt.checkpw 回验后写入。
-- 演示账号：
--   admin   / admin123    系统管理员（ADMIN）
--   mgr001  / mgr123456   计算机爱好者协会负责人（MANAGER）
--   mgr002  / mgr123456   街舞社负责人（MANAGER）
--   stu001~stu005 / stu123456  演示学生（STUDENT）
-- 首登提示：正式演示前请引导修改初始密码（01 文档 §5.1）。
-- =============================================================

USE association_system;

-- -------------------------------------------------------------
-- 1. 用户（id 固定，保证后续外键引用稳定）
-- -------------------------------------------------------------
INSERT IGNORE INTO sys_user (id, username, password, real_name, phone, role, avatar, status, deleted, created_at, updated_at) VALUES
(1, 'admin',  '$2a$10$nSTje.Orq402pY2f3rtxEOmxnl7vrFTBtH4M21aviLYtOjwHEBXre', '系统管理员', '13800000001', 'ADMIN',   NULL, 1, 0, NOW(), NOW()),
(2, 'mgr001', '$2a$10$qmNmBpkDAc8ybncdX62gEeXaSOt5pR.viJIvXYkHizajDw8HzkVbS', '陈志远',     '13800000002', 'MANAGER', NULL, 1, 0, NOW(), NOW()),
(3, 'mgr002', '$2a$10$1LwfbecZljk7hxoMPIx7huRjLIRdiDoHjF3lmg2x2aASI.nIhmgvG', '林晓东',     '13800000003', 'MANAGER', NULL, 1, 0, NOW(), NOW()),
(4, 'stu001', '$2a$10$aCoZSMFZyvCtf35rzXFavOr985jq1mWj5o4Cxd4EY/ytbVFllMCOu', '王小明',     '13900000001', 'STUDENT', NULL, 1, 0, NOW(), NOW()),
(5, 'stu002', '$2a$10$lntUURHCyz.B/IdFYYWlSuAfI0qEHs.6r6XidSJpE6mBrNf2A/me.', '李思思',     '13900000002', 'STUDENT', NULL, 1, 0, NOW(), NOW()),
(6, 'stu003', '$2a$10$rYpuwxQei9JwuNBSmtuA8ekZ/9xzzuHZc4BkIdO4utwNbfO2LWlEm', '张浩然',     '13900000003', 'STUDENT', NULL, 1, 0, NOW(), NOW()),
(7, 'stu004', '$2a$10$ZrKFWp5Z55dju1/8J3dxiOoTStR7CK08BKCObOB7eUaoCVqS7tbtG', '刘一诺',     '13900000004', 'STUDENT', NULL, 1, 0, NOW(), NOW()),
(8, 'stu005', '$2a$10$stDMxVStAQRLsyVMNHZW6OEOzFpihnUsg29NEH1.5lQTLgm0wI68C', '赵雨桐',     '13900000005', 'STUDENT', NULL, 1, 0, NOW(), NOW());

-- -------------------------------------------------------------
-- 2. 演示社团 2 个（绑定负责人）
-- -------------------------------------------------------------
INSERT IGNORE INTO association (id, name, code, category, leader_user_id, status, description, deleted, created_at, updated_at) VALUES
(1, '计算机爱好者协会', 'ASSOC-001', '学术', 2, 1, '面向全校的程序设计与技术交流社团，定期举办技术沙龙与竞赛培训。', 0, NOW(), NOW()),
(2, '街舞社',           'ASSOC-002', '文体', 3, 1, '校园街舞文化交流社团，涵盖 Breaking、Hip-hop 等舞种，常年组织公演。', 0, NOW(), NOW());

-- -------------------------------------------------------------
-- 3. 活动分类（文档 03 §6：文体活动、学术科技、志愿服务、社会实践）
-- -------------------------------------------------------------
INSERT IGNORE INTO activity_category (id, name, sort) VALUES
(1, '文体活动', 1),
(2, '学术科技', 2),
(3, '志愿服务', 3),
(4, '社会实践', 4);

-- -------------------------------------------------------------
-- 4. 演示活动 3 个（不同状态：报名中 / 已结束 / 已归档）
--    id 固定 + INSERT IGNORE 保证脚本可重复执行
-- -------------------------------------------------------------
INSERT IGNORE INTO activity (id, association_id, category_id, title, cover, description, location, start_time, end_time, signup_deadline, capacity, enrolled_count, status, audit_comment, created_by, deleted, created_at, updated_at) VALUES
(1, 1, 2, 'AI 大模型入门与实践分享会', NULL,
 '从零认识大语言模型：原理简介、Prompt 技巧与本地部署演示，欢迎带上笔记本现场实操。',
 '图书馆报告厅 A', '2026-10-20 19:00:00', '2026-10-20 21:00:00', '2026-10-19 23:59:59',
 50, 0, 'PUBLISHED', NULL, 2, 0, NOW(), NOW()),
(2, 1, 2, '程序设计竞赛校内选拔赛', NULL,
 '面向全校的编程能力比拼，优胜者将代表学校参加省级竞赛。比赛语言 C/C++/Java/Python 均可。',
 '第一教学楼机房 301', '2026-09-20 14:00:00', '2026-09-20 17:00:00', '2026-09-19 23:59:59',
 30, 0, 'ENDED', NULL, 2, 0, NOW(), NOW()),
(3, 2, 1, '街舞社秋季公演', NULL,
 '秋季学期成果公演，社团全员参与，欢迎全校师生到场观看。',
 '大学生活动中心剧场', '2026-06-10 18:30:00', '2026-06-10 20:30:00', '2026-06-09 23:59:59',
 200, 0, 'ARCHIVED', NULL, 3, 0, NOW(), NOW());

-- -------------------------------------------------------------
-- 5. 初始化记录（操作日志示例，便于管理端日志页非空验收）
-- -------------------------------------------------------------
INSERT IGNORE INTO operation_log (id, user_id, module, action, detail, ip, created_at) VALUES
(1, 1, 'admin', '初始化演示数据', '执行 db/data.sql 完成账号、社团、分类与演示活动初始化', '127.0.0.1', NOW());
