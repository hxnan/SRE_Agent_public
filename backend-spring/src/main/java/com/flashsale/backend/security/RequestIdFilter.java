package com.flashsale.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String rid = request.getHeader("X-Request-ID");
        if (rid == null || rid.isBlank()) rid = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, rid);
        response.setHeader("X-Request-ID", rid);
        MDC.put(REQUEST_ID, rid);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }
}

