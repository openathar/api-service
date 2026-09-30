package org.openathar.api.adapter.web;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the public developer portal (Swagger UI at
 * /swagger-ui.html, raw spec at /v3/api-docs). Kept separate from the
 * controllers so endpoint-level docs (tags, operations, examples) stay
 * next to the code they describe.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI atharOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Athar Public API")
                .description(
                    "Free, rate-limited REST API for prayer times, Qibla bearing and Hijri "
                        + "calendar conversion — part of the Athar platform (Sadaqah Jariyah). "
                        + "No API key, no account, no tracking. Results are deterministic for a "
                        + "given input and cached forever by clients and CDNs "
                        + "(`Cache-Control: public, max-age=31536000, immutable`) — except "
                        + "`/v1/hijri` without a `date`, which means \"today\" and is cached for "
                        + "5 minutes. Every error, on every endpoint, has the same shape: "
                        + "`{\"error\": \"...\"}`. "
                        + "Calculations are powered by "
                        + "[athan-core-java](https://github.com/openathar/athan-core-java), the "
                        + "same library used offline in the web and mobile apps, so server and "
                        + "client results always agree.")
                .version("v1")
                .contact(new Contact()
                    .name("Athar")
                    .url("https://openathar.org")
                    .email("hello@openathar.org"))
                .license(new License()
                    .name("MIT")
                    .url("https://github.com/openathar/api-service/blob/main/LICENSE")))
            .externalDocs(new ExternalDocumentation()
                .description("Athar on GitHub")
                .url("https://github.com/openathar"))
            .servers(List.of(
                new Server().url("https://api.openathar.org").description("Production"),
                new Server().url("http://localhost:8080").description("Local development")))
            .components(new Components()
                .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description(
                        "Optional. Get a free key via POST /v1/api-keys, then send it here for a "
                            + "higher rate limit than anonymous requests. Anonymous requests (no "
                            + "header) still work.")));
    }
}
