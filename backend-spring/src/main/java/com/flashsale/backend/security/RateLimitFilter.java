package com.flashsale.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final RateLimitConfigService config;

    public RateLimitFilter(StringRedisTemplate redis, RateLimitConfigService config) {
        this.redis = redis;
        this.config = config;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/seckill")) {
            String userId = extractUserId(request);
            String key = "seckill_rate_limit:" + (userId != null ? userId : request.getRemoteAddr());
            int win = config.getSeckillWindowSeconds();
            int max = config.getSeckillMax();
            if (!allow(key, Duration.ofSeconds(win), max)) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(win));
                response.setContentType("application/json;charset=UTF-8");
                String msg = String.format("{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Maximum %d requests per %d seconds.\",\"retryAfter\":%d}", max, win, win);
                response.getOutputStream().write(msg.getBytes(StandardCharsets.UTF_8));
                return;
            }
        } else if (path.startsWith("/api")) {
            String key = "api_rate_limit:" + request.getRemoteAddr();
            int win = config.getApiWindowSeconds();
            int max = config.getApiMax();
            if (!allow(key, Duration.ofSeconds(win), max)) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(win));
                response.setContentType("application/json;charset=UTF-8");
                String msg = String.format("{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Maximum %d requests per %d seconds.\",\"retryAfter\":%d}", max, win, win);
                response.getOutputStream().write(msg.getBytes(StandardCharsets.UTF_8));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean allow(String key, Duration window, int max) {
        Long v = redis.opsForValue().increment(key);
        if (v != null && v == 1L) redis.expire(key, window);
        return (v == null) || v <= max;
    }

    private String extractUserId(HttpServletRequest request) {
        Object principal = request.getUserPrincipal();
        if (principal instanceof org.springframework.security.core.Authentication auth) {
            Object details = auth.getPrincipal();
            if (details instanceof java.util.Map<?, ?> m) {
                Object id = m.get("userId");
                return id != null ? String.valueOf(id) : null;
            }
        }
        return null;
    }
}

