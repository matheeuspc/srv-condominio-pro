package com.mcardoso.srvcondominiopro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

// Origens liberadas para o front (React/Vercel/Lovable). Aceita padrões
// (setAllowedOriginPatterns), então "https://*.vercel.app" funciona.
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @DefaultValue({"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"})
        List<String> allowedOriginPatterns,

        @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
        List<String> allowedMethods,

        @DefaultValue("*")
        List<String> allowedHeaders,

        @DefaultValue("true")
        boolean allowCredentials,

        @DefaultValue("3600")
        long maxAgeSeconds
) {
}
