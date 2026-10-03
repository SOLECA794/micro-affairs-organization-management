package com.assoc.dto;

import jakarta.validation.constraints.NotNull;

/** 人工补签入参 */
public record ManualSigninDTO(
        @NotNull(message = "补签用户 ID 不能为空") Long userId) {
}
