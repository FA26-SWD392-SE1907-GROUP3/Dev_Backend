package com.example.swd392_se1907_aives.modules.identity.internal;



import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPassword {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = encoder.encode("admin123");

        System.out.println(hash);
    }
}
