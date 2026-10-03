package com.assoc.dto;

import jakarta.validation.constraints.Size;

/**
 * 管理员重置密码入参（实现期扩展）。
 * password 可空：留空时由服务端生成随机新密码；响应中明文返回一次。
 */
public record UserResetPasswordDTO(
        @Size(min = 8, max = 64, message = "密码长度 8-64 位")
        String password) {
}
