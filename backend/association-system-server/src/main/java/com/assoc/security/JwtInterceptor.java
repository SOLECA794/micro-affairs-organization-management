package com.assoc.security;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.common.ResponseWriter;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 认证与角色拦截器：
 * - 解析 Authorization: Bearer <token>，校验签名与有效期，写入 UserContext；失败返回 401；
 * - 按 @RequireRole 注解校验角色，不匹配返回 403；
 * - 公开端点：POST /api/auth/login、GET /api/activities（公开列表）、GET /api/signin/qrcode（扫码落地页）。
 */
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 公开接口放行
        boolean publicList = "GET".equals(method) && path.endsWith("/api/activities");
        boolean qrcodeLanding = "GET".equals(method) && path.endsWith("/api/signin/qrcode");
        if (publicList || qrcodeLanding) {
            return true;
        }

        // 认证
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            ResponseWriter.write(response, 401,
                    ApiResponse.error(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage()));
            return false;
        }
        String token = authorization.substring(7).trim();
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            ResponseWriter.write(response, 401,
                    ApiResponse.error(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage()));
            return false;
        }

        UserContext.LoginUser loginUser = new UserContext.LoginUser(
                UserContext.userIdOf(claims),
                claims.get("username", String.class),
                claims.get("realName", String.class),
                claims.get("role", String.class));
        UserContext.set(loginUser);

        // 角色校验：方法注解优先，其次类注解
        RequireRole required = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (required != null && !matchRole(required.value(), loginUser.role())) {
            ResponseWriter.write(response, 403,
                    ApiResponse.error(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage()));
            return false;
        }
        return true;
    }

    private boolean matchRole(String[] allowed, String actual) {
        for (String role : allowed) {
            if (role.equals(actual)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}
