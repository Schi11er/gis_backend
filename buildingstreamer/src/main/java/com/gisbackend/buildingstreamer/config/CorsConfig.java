package com.gisbackend.buildingstreamer.config;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Configuration - Temporär deaktiviert, da CorsFilter verwendet wird
public class CorsConfig {

    // @Bean - Temporär deaktiviert
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*") // Zurück zu allowedOrigins da keine Credentials
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Access-Control-Allow-Origin")
                        .maxAge(3600); // Cache preflight für 1 Stunde
            }
        };
    }
}