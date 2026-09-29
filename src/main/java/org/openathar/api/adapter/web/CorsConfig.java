package org.openathar.api.adapter.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for the public API: read-only GET/POST from the developer portal
 * (openathar.org and local dev) needs to call api.openathar.org directly
 * from the browser. All /v1/* responses are already public and cacheable,
 * so an open GET policy adds no real exposure; POST is limited to the API
 * key issuance endpoint.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(
            @Value("${athar.cors.allowed-origins:https://openathar.org,http://localhost:3000}") String origins) {
        this.allowedOrigins = origins.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v1/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST")
            .allowedHeaders("Content-Type", "X-API-Key")
            .maxAge(3600);
    }
}
