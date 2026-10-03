package com.assoc.dto;

import jakarta.validation.constraints.NotNull;

/** 启用/停用用户入参：status 1 启用，0 停用 */
public record UserStatusDTO(
        @NotNull(message = "状态不能为空") Integer status) {
}
