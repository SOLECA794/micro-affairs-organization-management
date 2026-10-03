package com.assoc.vo;

import java.time.LocalDateTime;

/** 用户信息出参（不含密码） */
public record UserVO(
        Long id,
        String username,
        String realName,
        String phone,
        String role,
        String avatar,
        Integer status,
        LocalDateTime createdAt) {
}
