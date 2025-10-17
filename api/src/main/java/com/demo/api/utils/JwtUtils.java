package com.demo.api.utils;

import com.demo.api.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
@Slf4j
public class JwtUtils {
    private final Key key;
    private final long expireTime;

    // 60分钟后认证过期
    public JwtUtils(@Value("${jwt.token.secret}") String secret,
                    @Value("${jwt.token.expireTime}") long expireTime) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireTime = expireTime;
    }

    /**
     * 生成 JWT
     * @param subject
     * @param claims
     * @return
     */
    public String generateJwt(String subject, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject) // subject 是用户id (String)
                .addClaims(claims == null ? Map.of() : claims) // claims 包括 username 和 email
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 JWT
     * @param token
     * @return
     */
    public Claims parse(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            long secondsLeft = Math.max(0, (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000);
            if (secondsLeft <= 600) {
                log.warn("Token has less than 10 minutes remaining: {} seconds", secondsLeft);
            } else {
                log.debug("Token remaining: {} seconds", secondsLeft);
            }
            return claims;

        } catch (ExpiredJwtException e) {
            throw new BusinessException("Token has expired");
        } catch (JwtException e) {
            throw new BusinessException("Invalid token");
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Token is null or wrong format");
        }
    }
}
