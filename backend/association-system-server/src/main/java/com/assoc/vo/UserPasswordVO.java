package com.assoc.vo;

/** 管理员创建用户 / 重置密码出参（实现期扩展）：一次性明文初始密码仅在此响应中出现 */
public record UserPasswordVO(
        Long id,
        String username,
        String realName,
        String role,
        String initialPassword) {
}
