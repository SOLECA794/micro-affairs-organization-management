package com.assoc.vo;

import java.time.LocalDateTime;

/** 审核列表项（待审核/已审核活动） */
public record AuditItemVO(
        Long id,
        String title,
        Long associationId,
        String associationName,
        String categoryName,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime signupDeadline,
        Integer capacity,
        String status,
        String auditComment,
        String description,
        LocalDateTime createdAt) {
}
