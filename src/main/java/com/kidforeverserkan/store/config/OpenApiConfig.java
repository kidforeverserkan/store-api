package com.kidforeverserkan.store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shoppingCartApi() {

        return new OpenAPI()
                .info(new Info()
                        .title("Shopping Cart API")
                        .description("""
                                A RESTful API built with Spring Boot for managing
                                shopping carts, products, and orders.

                                This project demonstrates REST API development,
                                layered architecture, Spring Data JPA, DTO mapping,
                                exception handling, validation, and OpenAPI
                                documentation.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Your Name")
                                .url("https://www.linkedin.com/in/your-linkedin")));
    }
}