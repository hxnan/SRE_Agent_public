package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.model.User;
import com.flashsale.backend.repository.UserRepository;
import com.flashsale.backend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoggerService log;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt, LoggerService log) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.log = log;
    }

    @PostMapping({"/login", "/mock/login"})
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }
        Optional<User> uOpt = users.findByUsername(username);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        User u = uOpt.get();
        if (!encoder.matches(password, u.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String token = jwt.generateToken(u.getId(), u.getUsername());
        Map<String, Object> resp = Map.of(
                "token", token,
                "expires_in", 7200,
                "user", Map.of("id", u.getId(), "username", u.getUsername())
        );
        java.util.Map<String,Object> ctx = new java.util.HashMap<>();
        ctx.put("requestId", String.valueOf(req.getAttribute("requestId")));
        ctx.put("userId", u.getId());
        ctx.put("username", u.getUsername());
        log.info("auth_login_success", ctx);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        var auth = (org.springframework.security.core.Authentication) req.getUserPrincipal();
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        Object principal = auth.getPrincipal();
        Long userId = principal instanceof java.util.Map<?, ?> m ? ((Number) m.get("userId")).longValue() : null;
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        Optional<User> uOpt = users.findById(userId);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        java.util.Map<String,Object> ctx = new java.util.HashMap<>();
        ctx.put("requestId", String.valueOf(req.getAttribute("requestId")));
        ctx.put("userId", uOpt.get().getId());
        log.info("auth_me_success", ctx);
        return ResponseEntity.ok(Map.of("user", uOpt.get()));
    }
}

