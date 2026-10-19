package com.example.swd392_se1907_aives.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AI-powered Viva Exam System (AIVES) API",
                version = "1.0.0",
                description = "Backend API for the AI-powered Viva Exam System (AIVES)",
                contact = @Contact(
                        name = "Group 3"
                )
        ),
        security = {
                @SecurityRequirement(name = "Bearer Authentication")
        }
)
@SecurityScheme(
        name = "Bearer Authentication",
        description = "JWT authentication token. Login to get your token.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}