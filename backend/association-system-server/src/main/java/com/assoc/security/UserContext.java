package com.assoc.security;

import io.jsonwebtoken.Claims;

/**
 * 当前登录用户上下文（拦截器写入，请求结束清理）。
 */
public final class UserContext {

    public record LoginUser(Long userId, String username, String realName, String role) {
    }

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    public static String role() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.role();
    }

    /** 从 JWT 负载解析 userId（JSON 数值可能为 Integer，统一转 Long） */
    public static Long userIdOf(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object value = claims.get("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
