package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员创建用户入参（实现期扩展）。
 * password 可空：留空时由服务端生成随机 8 位初始密码。
 */
public record UserCreateDTO(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,50}$", message = "用户名只能包含字母、数字、下划线，长度 3-50")
        String username,
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名最长 50 字")
        String realName,
        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "STUDENT|MANAGER", message = "角色仅允许 STUDENT 或 MANAGER")
        String role,
        @Size(max = 20, message = "手机号最长 20 字")
        String phone,
        String password) {
}
