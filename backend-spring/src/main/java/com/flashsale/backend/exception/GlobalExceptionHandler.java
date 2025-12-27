package com.flashsale.backend.exception;

import com.flashsale.backend.logging.LoggerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final LoggerService log;

    public GlobalExceptionHandler(LoggerService log) {
        this.log = log;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(HttpServletRequest req, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stack = sw.toString();
        java.util.Map<String,Object> ctx = new java.util.HashMap<>();
        ctx.put("method", req.getMethod());
        ctx.put("uri", req.getRequestURI());
        ctx.put("query", req.getQueryString());
        ctx.put("error", e.getMessage());
        ctx.put("stack", stack);
        ctx.put("requestId", String.valueOf(req.getAttribute("requestId")));
        log.error("unhandled_exception", ctx);
        return ResponseEntity.status(500).body(java.util.Map.of("error", "Internal server error"));
    }
}
