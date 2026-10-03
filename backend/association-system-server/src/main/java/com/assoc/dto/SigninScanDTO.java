package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 扫码签到入参（04 文档 §5.4） */
public record SigninScanDTO(
        @NotNull(message = "活动 ID 不能为空") Long activityId,
        @NotBlank(message = "签到码不能为空") String token) {
}
