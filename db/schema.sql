-- =============================================================
-- 社团活动报名与签到系统 · 建表脚本（可重复执行）
-- 依据：03-数据库设计.md（9 张表、唯一约束、索引、CHECK）
-- 数据库：MySQL 8，InnoDB，utf8mb4_general_ci
-- =============================================================

CREATE DATABASE IF NOT EXISTS association_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE association_system;

-- -------------------------------------------------------------
-- 1. sys_user 用户（学生/负责人/管理员）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    real_name   VARCHAR(50)  NOT NULL COMMENT '姓名',
    phone       VARCHAR(20)  NULL COMMENT '手机号',
    role        VARCHAR(20)  NOT NULL COMMENT 'STUDENT / MANAGER / ADMIN',
    avatar      VARCHAR(255) NULL COMMENT '头像地址',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常，0 停用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
    created_at  DATETIME     NOT NULL COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

-- -------------------------------------------------------------
-- 2. association 社团
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS association (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '社团 ID',
    name           VARCHAR(100) NOT NULL COMMENT '社团名称',
    code           VARCHAR(30)  NOT NULL COMMENT '社团编号',
    category       VARCHAR(50)  NULL COMMENT '社团分类（文本，如文体/学术）',
    leader_user_id BIGINT       NOT NULL COMMENT '负责人用户 ID，关联 sys_user',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常，0 停用',
    description    VARCHAR(500) NULL COMMENT '简介',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
    created_at     DATETIME     NOT NULL COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assoc_code (code),
    KEY idx_assoc_leader (leader_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社团表';

-- -------------------------------------------------------------
-- 3. activity_category 活动分类
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_category (
    id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名',
    sort INT         NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '活动分类表';

-- -------------------------------------------------------------
-- 4. activity 活动
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '活动 ID',
    association_id  BIGINT       NOT NULL COMMENT '所属社团，关联 association',
    category_id     BIGINT       NULL COMMENT '分类，关联 activity_category',
    title           VARCHAR(100) NOT NULL COMMENT '活动标题',
    cover           VARCHAR(255) NULL COMMENT '封面地址',
    description     TEXT         NULL COMMENT '活动简介',
    location        VARCHAR(200) NOT NULL COMMENT '地点',
    start_time      DATETIME     NOT NULL COMMENT '开始时间',
    end_time        DATETIME     NOT NULL COMMENT '结束时间',
    signup_deadline DATETIME     NOT NULL COMMENT '报名截止时间',
    capacity        INT          NOT NULL COMMENT '名额上限',
    enrolled_count  INT          NOT NULL DEFAULT 0 COMMENT '当前报名成功数（含占位计数）',
    status          VARCHAR(20)  NOT NULL COMMENT 'DRAFT/PENDING/APPROVED/REJECTED/PUBLISHED/CANCELLED/ENDED/ARCHIVED',
    audit_comment   VARCHAR(500) NULL COMMENT '审核意见',
    created_by      BIGINT       NOT NULL COMMENT '创建人（负责人）',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
    created_at      DATETIME     NOT NULL COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_act_assoc (association_id),
    KEY idx_act_status (status),
    KEY idx_act_start (start_time),
    CONSTRAINT chk_act_capacity CHECK (capacity > 0),
    CONSTRAINT chk_act_deadline CHECK (signup_deadline <= start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '活动表';

-- -------------------------------------------------------------
-- 5. signup 报名（含候补）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS signup (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '报名 ID',
    activity_id BIGINT      NOT NULL COMMENT '活动，关联 activity',
    user_id     BIGINT      NOT NULL COMMENT '用户，关联 sys_user',
    status      VARCHAR(20) NOT NULL COMMENT 'ACTIVE（报名成功）/ WAITING（候补）/ CANCELLED（已取消）',
    queue_order INT         NOT NULL DEFAULT 0 COMMENT '候补序号（非候补为 0）',
    signup_time DATETIME    NOT NULL COMMENT '报名时间',
    cancel_time DATETIME    NULL COMMENT '取消时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_signup (activity_id, user_id),
    KEY idx_signup_activity (activity_id, status),
    KEY idx_signup_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '报名表';

-- -------------------------------------------------------------
-- 6. attendance 签到
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '签到 ID',
    activity_id BIGINT      NOT NULL COMMENT '活动',
    user_id     BIGINT      NOT NULL COMMENT '用户',
    sign_type   VARCHAR(20) NOT NULL COMMENT 'QR（扫码）/ MANUAL（补签）',
    sign_time   DATETIME    NOT NULL COMMENT '签到时间',
    operator_id BIGINT      NULL COMMENT '补签操作人（扫码为空）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance (activity_id, user_id),
    KEY idx_att_activity (activity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '签到表';

-- -------------------------------------------------------------
-- 7. signin_code 签到码（限时二维码）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS signin_code (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '签到码 ID',
    activity_id BIGINT      NOT NULL COMMENT '活动',
    token       VARCHAR(64) NOT NULL COMMENT '随机 Token（UUID）',
    expires_at  DATETIME    NOT NULL COMMENT '过期时间',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 有效，0 已作废',
    created_by  BIGINT      NOT NULL COMMENT '开启人',
    created_at  DATETIME    NOT NULL COMMENT '生成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_token (token),
    KEY idx_code_activity (activity_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '签到码表';

-- -------------------------------------------------------------
-- 8. notification 站内通知
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知 ID',
    user_id     BIGINT       NOT NULL COMMENT '接收人',
    type        VARCHAR(30)  NOT NULL COMMENT 'AUDIT/SIGNUP/WAITING/PROMOTED/CANCELLED/SIGNIN',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    content     VARCHAR(500) NULL COMMENT '内容',
    read_status TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未读，1 已读',
    created_at  DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_notify_user (user_id, read_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '站内通知表';

-- -------------------------------------------------------------
-- 9. operation_log 操作日志
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志 ID',
    user_id    BIGINT       NULL COMMENT '操作人',
    module     VARCHAR(30)  NOT NULL COMMENT '模块（auth/signup/signin/activity/admin）',
    action     VARCHAR(50)  NOT NULL COMMENT '动作描述',
    detail     VARCHAR(500) NULL COMMENT '详情',
    ip         VARCHAR(50)  NULL COMMENT '来源 IP',
    created_at DATETIME     NOT NULL COMMENT '时间',
    PRIMARY KEY (id),
    KEY idx_log_user (user_id),
    KEY idx_log_time (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表';
