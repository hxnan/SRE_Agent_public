package com.flashsale.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiresIn;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expires-in}") String expires) {
        // Accept plain string secret; if base64, decode; otherwise use bytes
        byte[] secretBytes;
        try {
            secretBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            secretBytes = secret.getBytes();
        }
        this.key = Keys.hmacShaKeyFor(secretBytes.length < 32 ? pad(secretBytes) : secretBytes);
        this.expiresIn = parseDuration(expires);
    }

    private static byte[] pad(byte[] src) {
        byte[] dst = new byte[32];
        System.arraycopy(src, 0, dst, 0, Math.min(src.length, 32));
        for (int i = src.length; i < 32; i++) dst[i] = (byte) '0';
        return dst;
    }

    private static Duration parseDuration(String expr) {
        if (expr == null || expr.isBlank()) return Duration.ofHours(2);
        if (expr.endsWith("h")) return Duration.ofHours(Long.parseLong(expr.substring(0, expr.length() - 1)));
        if (expr.endsWith("m")) return Duration.ofMinutes(Long.parseLong(expr.substring(0, expr.length() - 1)));
        if (expr.endsWith("s")) return Duration.ofSeconds(Long.parseLong(expr.substring(0, expr.length() - 1)));
        return Duration.ofHours(2);
    }

    public String generateToken(long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiresIn.toMillis());
        return Jwts.builder()
                .setClaims(Map.of("userId", userId, "username", username))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}

