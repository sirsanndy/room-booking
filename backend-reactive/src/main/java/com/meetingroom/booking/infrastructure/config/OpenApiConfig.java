package com.meetingroom.booking.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Meeting Room Booking API (Reactive)",
        version = "1.0.0",
        description = "Reactive REST API for Meeting Room Booking System with Hexagonal Architecture",
        contact = @Contact(
            name = "API Support",
            email = "support@meetingroom.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"
        )
    ),
    servers = {
        @Server(
            description = "Local Development Server",
            url = "http://localhost:8081"
        ),
        @Server(
            description = "Production Server",
            url = "https://api-reactive.meetingroom.com"
        )
    },
    security = {
        @SecurityRequirement(name = "Bearer Authentication")
    }
)
@SecurityScheme(
    name = "Bearer Authentication",
    description = "JWT Bearer token authentication. Please login first to get the token.",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
