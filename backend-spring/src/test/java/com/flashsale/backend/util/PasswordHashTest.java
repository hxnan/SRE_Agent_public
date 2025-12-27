package com.flashsale.backend.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;
import java.io.FileWriter;

public class PasswordHashTest {
    @Test
    void writeBcrypts() throws Exception {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String u1 = "testuser";
        String p1 = "test123";
        String u2 = "admin";
        String p2 = "admin123";
        String h1 = enc.encode(p1);
        String h2 = enc.encode(p2);
        File f = new File("target/bcrypt.txt");
        try (FileWriter w = new FileWriter(f, false)) {
            w.write(u1 + ":" + p1 + ":" + h1 + "\n");
            w.write(u2 + ":" + p2 + ":" + h2 + "\n");
        }
    }
}
