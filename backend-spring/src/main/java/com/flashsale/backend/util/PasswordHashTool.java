package com.flashsale.backend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTool {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        if (args == null || args.length == 0) {
            System.out.println(enc.encode("test123"));
            System.out.println(enc.encode("admin123"));
            return;
        }
        for (String p : args) {
            System.out.println(enc.encode(p));
        }
    }
}
