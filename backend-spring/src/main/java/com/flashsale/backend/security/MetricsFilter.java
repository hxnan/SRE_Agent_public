package com.flashsale.backend.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MetricsFilter extends OncePerRequestFilter {
    private final MeterRegistry registry;

    public MetricsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
        Counter.builder("http_requests_total")
                .tag("method", request.getMethod())
                .tag("route", request.getRequestURI())
                .tag("status", String.valueOf(response.getStatus()))
                .register(registry)
                .increment();
    }
}

