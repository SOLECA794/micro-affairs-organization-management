package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改本人密码入参 */
public record ChangePasswordDTO(
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "新密码长度需为 6-32 位") String newPassword) {
}
