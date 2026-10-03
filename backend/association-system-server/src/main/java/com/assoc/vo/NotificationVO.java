package com.assoc.vo;

import java.time.LocalDateTime;

/** 站内通知项 */
public record NotificationVO(
        Long id,
        String type,
        String title,
        String content,
        Integer readStatus,
        LocalDateTime createdAt) {
}
