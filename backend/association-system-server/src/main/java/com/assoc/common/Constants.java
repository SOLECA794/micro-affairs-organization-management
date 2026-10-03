package com.assoc.common;

/**
 * 项目通用常量：角色、活动状态、报名状态、通知类型、签到方式等（状态统一语义值大写）。
 */
public final class Constants {

    private Constants() {
    }

    /** 角色 */
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_ADMIN = "ADMIN";

    /** 活动状态（02 文档 §5.2 状态机） */
    public static final String ACT_DRAFT = "DRAFT";
    public static final String ACT_PENDING = "PENDING";
    public static final String ACT_APPROVED = "APPROVED";
    public static final String ACT_REJECTED = "REJECTED";
    public static final String ACT_PUBLISHED = "PUBLISHED";
    public static final String ACT_CANCELLED = "CANCELLED";
    public static final String ACT_ENDED = "ENDED";
    public static final String ACT_ARCHIVED = "ARCHIVED";

    /** 报名状态 */
    public static final String SIGNUP_ACTIVE = "ACTIVE";
    public static final String SIGNUP_WAITING = "WAITING";
    public static final String SIGNUP_CANCELLED = "CANCELLED";

    /** 签到方式 */
    public static final String SIGN_TYPE_QR = "QR";
    public static final String SIGN_TYPE_MANUAL = "MANUAL";

    /** 通知类型（03 文档 notification.type） */
    public static final String NOTIFY_AUDIT = "AUDIT";
    public static final String NOTIFY_SIGNUP = "SIGNUP";
    public static final String NOTIFY_WAITING = "WAITING";
    public static final String NOTIFY_PROMOTED = "PROMOTED";
    public static final String NOTIFY_CANCELLED = "CANCELLED";
    public static final String NOTIFY_SIGNIN = "SIGNIN";

    /** 日志模块 */
    public static final String MODULE_AUTH = "auth";
    public static final String MODULE_SIGNUP = "signup";
    public static final String MODULE_SIGNIN = "signin";
    public static final String MODULE_ACTIVITY = "activity";
    public static final String MODULE_ADMIN = "admin";

    /** 通用：启用/停用 */
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    /** 分页边界（04 文档：page 从 1 起，size 默认 10，最大 100） */
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;
}
