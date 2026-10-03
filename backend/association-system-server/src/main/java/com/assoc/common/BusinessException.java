package com.assoc.common;

/**
 * 业务异常：携带统一错误码与提示信息，由全局异常处理器转换为统一响应。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
