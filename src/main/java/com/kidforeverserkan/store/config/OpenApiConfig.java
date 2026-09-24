package com.kidforeverserkan.store.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Store API")
                                .version("1.0.0")
                                .description(
                                        "A RESTful API built with Spring Boot for managing " +
                                                "shopping carts, products, and orders.\n\n" +
                                                "This project demonstrates REST API development, " +
                                                "layered architecture, Spring Data JPA, DTO mapping, " +
                                                "exception handling, validation, JWT authentication, " +
                                                "and OpenAPI documentation."
                                )
                                .contact(
                                        new Contact()
                                                .name("Serkan")
                                                .url("https://www.linkedin.com/in/serkan-eyigun-a9b011198/")
                                )
                )

                // Apply JWT authentication to API endpoints.
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearerAuth")
                )

                // Define the JWT Bearer authentication scheme.
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                );
    }
}