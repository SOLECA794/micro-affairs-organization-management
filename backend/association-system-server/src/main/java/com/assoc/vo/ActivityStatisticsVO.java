package com.assoc.vo;

/** 活动统计（口径：到场率 = 签到人数 / 报名成功人数 × 100%） */
public record ActivityStatisticsVO(
        Long activityId,
        String title,
        Integer enrolledCount,
        Integer signedCount,
        Integer absentCount,
        Double attendanceRate) {
}
