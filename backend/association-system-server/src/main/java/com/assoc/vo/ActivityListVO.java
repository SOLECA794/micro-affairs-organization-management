package com.assoc.vo;

import java.time.LocalDateTime;

/** 活动列表项（04 文档 §5.1 字段） */
public record ActivityListVO(
        Long id,
        String title,
        String associationName,
        String categoryName,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime signupDeadline,
        Integer capacity,
        Integer enrolledCount,
        String status) {
}
