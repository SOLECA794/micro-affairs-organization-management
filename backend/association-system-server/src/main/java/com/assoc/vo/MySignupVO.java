package com.assoc.vo;

import java.time.LocalDateTime;

/** 我的报名列表项 */
public record MySignupVO(
        Long signupId,
        Long activityId,
        String activityTitle,
        String associationName,
        String location,
        LocalDateTime startTime,
        String activityStatus,
        String status,
        Integer queueOrder,
        LocalDateTime signupTime,
        LocalDateTime cancelTime) {
}
