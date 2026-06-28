package com.example.demo.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private final ConcurrentMap<String, Date> revokedTokens = new ConcurrentHashMap<>();

    //產生token，包含使用者的 email 和 role，並設定過期時間
    public String generateToken(String email, String role, LocalDateTime expiresAt) {
        Date now = new Date();
        Date expiration = Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public LocalDateTime calculateExpiration() {
        return LocalDateTime.now().plusNanos(expirationMs * 1_000_000);
    }
    //驗證 token 是否有效，檢查簽名和過期時間
    public boolean isTokenValid(String token) {
        try {
            removeExpiredRevokedTokens();
            if (isTokenRevoked(token)) {
                return false;
            }

            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public void revokeToken(String token) {
        Claims claims = parseClaims(token);
        revokedTokens.put(token, claims.getExpiration());
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public LocalDateTime getExpiration(String token) {
        return LocalDateTime.ofInstant(
                parseClaims(token).getExpiration().toInstant(),
                ZoneId.systemDefault());
    }

    public String extractTokenFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    //解析 token，返回其中的 claims（如 email 和 role）
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenRevoked(String token) {
        Date expiration = revokedTokens.get(token);
        if (expiration == null) {
            return false;
        }

        if (!expiration.after(new Date())) {
            revokedTokens.remove(token);
            return false;
        }

        return true;
    }

    private void removeExpiredRevokedTokens() {
        Date now = new Date();
        revokedTokens.entrySet().removeIf(entry -> !entry.getValue().after(now));
    }

    //根據 jwtSecret 生成簽名密鑰，用於簽署和驗證 token
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
