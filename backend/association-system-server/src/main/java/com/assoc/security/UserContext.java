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

    /** 从 JWT 负载解析 userId（部署阶段修复：签发端将 userId 写入 subject，此处改为读 subject；
     *  原实现读取不存在的 "userId" 声明，导致所有业务接口拿到 null 用户） */
    public static Long userIdOf(Claims claims) {
        if (claims == null) {
            return null;
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            return null;
        }
        return Long.valueOf(subject);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
