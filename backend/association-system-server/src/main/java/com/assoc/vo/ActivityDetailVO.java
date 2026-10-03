package com.assoc.vo;

import java.time.LocalDateTime;

/** 活动详情：含剩余名额、当前用户报名状态 */
public record ActivityDetailVO(
        Long id,
        String title,
        Long associationId,
        Long categoryId,
        String associationName,
        String categoryName,
        String cover,
        String description,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime signupDeadline,
        Integer capacity,
        Integer enrolledCount,
        Integer remainingCount,
        String status,
        String auditComment,
        String mySignupStatus,
        Integer myQueueOrder,
        Boolean canSignup,
        Boolean canCancel) {
}
