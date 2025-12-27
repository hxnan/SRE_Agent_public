package com.flashsale.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        String token = extractToken(auth);
        if (token != null) {
            try {
                var claims = jwtService.parse(token);
                var principal = Map.of(
                        "userId", ((Number) claims.get("userId")).longValue(),
                        "username", (String) claims.get("username")
                );
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, null);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(String header) {
        if (!StringUtils.hasText(header)) return null;
        var parts = header.trim().split(" ");
        if (parts.length == 2 && parts[0].equalsIgnoreCase("Bearer")) return parts[1];
        return null;
    }
}

