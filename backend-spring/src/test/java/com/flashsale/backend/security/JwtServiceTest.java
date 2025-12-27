package com.flashsale.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {com.flashsale.backend.FlashsaleBackendApplication.class})
class JwtServiceTest {
    @Autowired
    JwtService jwtService;

    @Test
    void generateAndParse() {
        String token = jwtService.generateToken(1L, "admin");
        var claims = jwtService.parse(token);
        assertEquals(1L, ((Number) claims.get("userId")).longValue());
        assertEquals("admin", claims.get("username"));
    }
}

