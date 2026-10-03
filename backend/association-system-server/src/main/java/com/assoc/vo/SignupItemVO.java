package com.assoc.vo;

import java.time.LocalDateTime;

/** 报名名单项（社团端，按状态筛选、分页） */
public record SignupItemVO(
        Long signupId,
        Long userId,
        String realName,
        String phone,
        String status,
        Integer queueOrder,
        LocalDateTime signupTime,
        Boolean signed) {
}
