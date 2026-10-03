package com.assoc.vo;

import java.time.LocalDateTime;

/** 签到名单项 */
public record AttendanceItemVO(
        Long attendanceId,
        Long userId,
        String realName,
        String phone,
        String signType,
        LocalDateTime signTime,
        String operatorName) {
}
