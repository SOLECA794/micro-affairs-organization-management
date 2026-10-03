package com.assoc.vo;

import java.time.LocalDateTime;

/** 操作日志项 */
public record LogVO(
        Long id,
        Long userId,
        String operatorName,
        String module,
        String action,
        String detail,
        String ip,
        LocalDateTime createdAt) {
}
