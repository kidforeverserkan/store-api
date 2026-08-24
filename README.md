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

```

## Configuration

The application uses environment variables for sensitive configuration.

Create a `.env` file using `.env.example` as a template.

Example:

```env
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
JWT_SECRET=your_jwt_secret
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_WEBHOOK_SECRET_KEY=your_stripe_webhook_secret_key
```

Never commit the real `.env` file or API keys to Git.

## Database

The application uses MySQL.

The database is:

```text
store_api
```

Flyway is used to manage database migrations.

## Running the Application

### 1. Clone the repository

```bash
git clone <your-github-repository-url>
cd store-api
```

### 2. Configure environment variables

Create a `.env` file using `.env.example` as a template.

Add your local database, JWT, and Stripe configuration.

### 3. Run the application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The application runs at:

http://localhost:8080

## API Documentation

### Local Development

Swagger UI is available locally when the application is running:

http://localhost:8080/swagger-ui/index.html

### Live API Documentation

The deployed Swagger UI is available at:

https://store-api-production-a508.up.railway.app/swagger-ui/index.html

## Payments

Stripe Checkout is used to process payments.

The application also handles Stripe webhooks to update an order's payment status after a successful or failed payment.

For local development, Stripe test-mode credentials should be used.

## Testing

Run the tests with:

```powershell
.\mvnw.cmd clean test
```

## Project Status

Completed portfolio project.

This project demonstrates practical experience with Java, Spring Boot, REST APIs, databases, authentication, authorization, payment processing, testing, and clean project structure.

## Acknowledgements

This project was originally started while following a Spring Boot course and has since been extended, refactored, and customized as a personal portfolio project.
