package com.researchagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration for Spring Boot Application
 * Allows cross-origin requests from frontend applications
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://*",
                        "https://*"
                )
                // Allow all HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // Allow all headers
                .allowedHeaders("*")
                // Allow credentials (cookies, authorization headers)
                .allowCredentials(true)
                // How long preflight response can be cached (in seconds)
                .maxAge(3600);
    }
}
