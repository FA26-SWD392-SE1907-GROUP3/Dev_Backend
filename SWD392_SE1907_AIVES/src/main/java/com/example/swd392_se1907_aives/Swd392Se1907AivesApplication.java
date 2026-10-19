package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.config.LocalEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Swd392Se1907AivesApplication {
    public static void main(String[] args) {
        // Load .env next to pom.xml regardless of IntelliJ's working directory.
        // Actual environment variables and -D properties take precedence.
        LocalEnvLoader.load();
        SpringApplication.run(Swd392Se1907AivesApplication.class, args);
    }
}
