# Store API

A Spring Boot e-commerce REST API built as a backend portfolio project.

The application provides user authentication, product and cart management, order processing, and Stripe payment integration. It uses MySQL for persistence and Flyway for database migrations.

## Live Demo

The API is deployed on Railway.

Swagger UI:

https://store-api-production-a508.up.railway.app/swagger-ui/index.html

## Features

- User registration and authentication
- JWT-based authentication
- Access and refresh tokens
- Product management
- Shopping cart management
- Order management
- Role-based authorization
- Password management
- Stripe Checkout integration
- Stripe payment webhooks
- Payment status tracking
- MySQL database
- Flyway database migrations
- DTOs and MapStruct mappings
- Global exception handling
- Bean validation
- OpenAPI / Swagger documentation
- Thymeleaf landing page
- Automated tests

## Tech Stack

- Java 23
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- Flyway
- Stripe
- JWT
- MapStruct
- Lombok
- Maven
- Thymeleaf
- OpenAPI / Swagger

## Project Structure

The application follows a layered architecture:

```text
src/main/java/com/kidforeverserkan/store
├── config
├── controllers
├── dtos
├── entities
├── exceptions
├── filters
├── mappers
├── payments
├── repositories
├── service
└── validation
