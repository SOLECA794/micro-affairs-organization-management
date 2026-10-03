package com.assoc.security;

import com.assoc.common.ErrorCode;
import com.assoc.common.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析（负载含 userId、role），HS256。
 */
public class JwtUtil {

    private final String secret;
    private final long expireHours;
    private SecretKey key;

    public JwtUtil(String secret, long expireHours) {
        this.secret = secret;
        this.expireHours = expireHours;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥未配置或长度不足 32 字节，请检查 JWT_SECRET 环境变量");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 Token：subject = userId，负载含 username/realName/role */
    public String issue(Long userId, String username, String realName, String role) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("realName", realName)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key)
                .compact();
    }

    /** 校验并解析 Token，失效抛业务异常（40100） */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage());
        }
    }

    public static Long userIdOf(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }
}
