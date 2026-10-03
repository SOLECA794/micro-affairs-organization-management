package com.assoc.common;

/**
 * 统一错误码（04 文档 §3）。
 */
public enum ErrorCode {

    /** 参数校验错误（业务参数），40001–40099 */
    PARAM_ERROR(40001, "参数错误"),

    /** 未登录或 Token 失效 */
    UNAUTHORIZED(40100, "未登录或登录已失效"),

    /** 无权限（角色越权） */
    FORBIDDEN(40300, "无权限执行该操作"),

    /** 资源不存在 */
    NOT_FOUND(40400, "资源不存在"),

    /** 重复操作（重复报名/重复签到） */
    DUPLICATE(40901, "重复操作"),

    /** 名额已满 */
    QUOTA_FULL(40902, "名额已满"),

    /** 状态不允许（活动状态/时间不满足） */
    STATE_NOT_ALLOWED(40903, "当前状态不允许该操作"),

    /** 系统异常（兜底） */
    SYSTEM_ERROR(50000, "系统异常，请稍后重试");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
