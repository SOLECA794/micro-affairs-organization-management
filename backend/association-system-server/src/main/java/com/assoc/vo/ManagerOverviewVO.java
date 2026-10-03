package com.assoc.vo;

import java.util.List;

/** 社团端工作台统计概览 */
public record ManagerOverviewVO(
        Long associationId,
        String associationName,
        Long activityTotal,
        Long publishedTotal,
        Long signupTotal,
        Long attendanceTotal,
        List<ActivityListVO> recentActivities) {
}
