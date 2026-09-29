package com.gis.logistics.common.security;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与校验（权限关键路径，需人工复核）。
 * 载荷：sub=userId, role, iss, iat, exp。密钥从环境变量注入，缺失 fail-fast。
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long expireSeconds;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.issuer:gis-logistics}") String issuer,
                      @Value("${app.jwt.expire-seconds:86400}") long expireSeconds) {
        String key = (secret == null || secret.isBlank())
                ? System.getenv("JWT_SECRET")
                : secret;
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("JWT secret is required (app.jwt.secret or JWT_SECRET env)");
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // 归一化到 32 字节，HS256 要求 >= 32
            byte[] normalized = new byte[32];
            System.arraycopy(keyBytes, 0, normalized, 0, keyBytes.length);
            keyBytes = normalized;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.expireSeconds = expireSeconds;
    }

    public String issue(Long userId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireSeconds * 1000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    @Getter
    public static class Claims {
        private final Long userId;
        private final String role;

        public Claims(Long userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }

    public Claims parse(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);
            return new Claims(userId, role);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BizException(ErrorCode.AUTH_TOKEN_EXPIRED, "token expired");
        } catch (Exception e) {
            throw new BizException(ErrorCode.AUTH_TOKEN_INVALID, "invalid token");
        }
    }
}
