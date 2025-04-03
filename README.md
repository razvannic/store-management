# 🛒 Store Management Microservice

A clean-architecture-based backend microservice built with **Java 17**, **Spring Boot**, **JWT security**, and **TDD-first** development practices.

This service manages products in a store and includes basic authentication with role-based access control. It is designed using Onion Architecture for high modularity and testability.

---

## ✅ Features Implemented

| Feature                        | Status | Notes                                      |
|--------------------------------|--------|--------------------------------------------|
| Product entity & service       | ✅     | Built using TDD, tested with JUnit 5       |
| Onion architecture             | ✅     | Separate domain, application, infra layers |
| H2 local dev DB                | ✅     | Runs in-memory with no setup required      |
| JWT authentication             | ✅     | Stateless, secure tokens via `jjwt`        |
| Role-based access control      | ✅     | `ROLE_ADMIN` and `ROLE_USER` supported     |
| Token-based login endpoint     | ✅     | `/auth/login` issues JWTs                  |
| Full CRUD with auth protection | ✅     | `POST`, `GET`, `PUT`, `DELETE`             |
| Product unit tests             | ✅     | Covers all public methods                  |
| Authentication unit tests      | ✅     | Covers authentication methods              |
| Integration unit tests         | ✅     | Covers product creation and authentication |
| Input validation with Jakarta  | ✅     | `@NotBlank`, `@Min`, etc. on DTO fields    |
| Global exception handling      | ✅     | Custom `@ControllerAdvice` implementation  |
| Configurable token expiration  | ✅     | via `application.properties`               |
| Caching with Spring & Caffeine | ✅     | `findById`, `update`, `delete` optimized   |

---

## 🧱 Project Architecture

This project follows a **Clean / Onion Architecture** style:

```
web.controller              --> Exposes REST APIs  
application.service         --> Implements business logic  
application.mapper          --> Maps DTOs ↔ Domain ↔ Entities  
domain.model                --> Pure models (no annotations)  
domain.repository           --> Abstract interfaces  
infrastructure.persistence  --> JPA entities and adapters  
```

---

## 🛠️ Technology Stack

- Java 17
- Spring Boot 3.2.x
- Spring Security 6
- Maven
- H2 database (dev profile)
- JWT (JJWT 0.11.5)
- Jakarta Validation
- Lombok
- JUnit 5 + Mockito
- Spring Cache with Caffeine

---

## 🧪 Product Service Coverage

### 🔹 Summary

- Bootstrapped with Maven & Java 17
- Created `ProductRequestDto`, `ProductResponseDto`, `Product`, and `ProductEntity`
- Used TDD to implement and verify service methods

### ✅ Covered Service Methods

| Method            | Test                                  |
|-------------------|---------------------------------------|
| `addProduct()`    | Verifies DTO → domain → save          |
| `findById()`      | Tests success and not-found exception |
| `findAll()`       | Returns all mocked products           |
| `updateProduct()` | Validates update + save               |
| `deleteProduct()` | Verifies `deleteById()` called        |

---

## 🔐 JWT Authentication & Role-based Access

### 🔹 Summary

- `/auth/login` returns JWT tokens (with username and role claims)
- Spring Security config validates JWT in `Authorization: Bearer <token>` header
- Secured endpoints require valid roles:
    - `ROLE_ADMIN` → full access
    - `ROLE_USER` → read-only
- Expiration configured in `application.properties` as `jwt.expiration=3600000`

### 🧪 Login Example

```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}
```

Returns:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600000
}
```

---

## 🔁 Integration Testing

### 🔬 ProductIntegrationTest

```java
@Test
void shouldCreateProductAsAdmin() throws Exception {
  final var token = getAuthToken("admin", "admin");

  mockMvc.perform(post("/products")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                              {
                                  "name": "Keyboard",
                                  "price": 49.99,
                                  "quantity": 20
                              }
                          """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("Keyboard"));
}
```

- Uses real JWT authentication and H2 in-memory DB
- Includes: create, fetch, update, delete, unauthorized scenarios

---

## ❗ Global Exception Handling

- Implemented via `@ControllerAdvice`
- Returns structured JSON error for:
    - `ProductNotFoundException` → 404
    - `MethodArgumentNotValidException` → 400
    - Unhandled exceptions → 500

Sample 404 response:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product with id 999 was not found."
}
```

---

## 🧹 Input Validation

Validation added using `jakarta.validation.constraints` in `ProductRequestDto`.

```java
public record ProductRequestDto(
    @NotBlank(message = "Product name must not be blank")
    String name,

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    double price,

    @Min(value = 1, message = "Quantity must be at least 1")
    int quantity
) {}
```

On error:

```json
{
  "status": 400,
  "error": "Bad Request",
  "messages": [
    { "field": "price", "message": "Price must be greater than 0" },
    ...
  ]
}
```

---

## ⚡ Caching with Caffeine

Enabled caching for:

- `findById()` → returns cached product
- `updateProduct()` → manually updates cache
- `deleteProduct()` → evicts from cache

Cache config:

```properties
spring.cache.type=caffeine
spring.cache.cache-names=product,products
spring.cache.caffeine.spec=maximumSize=100,expireAfterWrite=10s
```

Tested via `ProductServiceCachingTest`:

```java
@Test
void shouldUseCacheInFindById() {
  Product saved = productRepository.save(new Product(null, "Monitor", 150.0, 5));
  Product first = productService.findById(saved.getId());
  productRepository.deleteById(saved.getId());
  Product second = productService.findById(saved.getId());
  assertEquals("Monitor", second.getName()); // from cache
}
```

---

## ⚙️ Configuration: application.properties

```properties
# H2 DB
spring.datasource.url=jdbc:h2:mem:storedb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true

# JWT
jwt.secret=kdiRmEFAxj2fnVqtu9nqx7BQox3Jr2VyNTOccDFRAK8=
jwt.expiration=3600000

# Caching
spring.cache.type=caffeine
spring.cache.cache-names=product,products
spring.cache.caffeine.spec=maximumSize=100,expireAfterWrite=10s
```

---

---

## 🐘 Postgres Setup (MacOS)

You can configure a local PostgreSQL instance to run the microservice in a real database environment instead of H2.

### 🔧 Installation (via Homebrew)

```bash
brew install postgresql
brew services start postgresql
```

Create the DB:
```sql
createdb storedb
```

Check it works:
```sql
psql store_db
```

Then run a sample query:
```sql
\dt       -- see tables

-- Create user
CREATE USER store_user WITH PASSWORD 'store_pass';

-- Create database
CREATE DATABASE store_db OWNER store_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE store_db TO store_user;

\c store_db store_user

CREATE TABLE users (
 id SERIAL PRIMARY KEY,
 username VARCHAR(50) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL,
 role VARCHAR(20) NOT NULL
);

INSERT INTO users (username, password, role) VALUES
                                               ('admin', '$2a$10$9rd8UwWOqE1OGk3THNmlouOeNlf41sScbrpsKwEtEz/OL6AcZrBry', 'ROLE_ADMIN'),
                                               ('user',  '$2a$10$vVQBCU7JSWLE6whk88AyQ.wTEtnEeSsZr9DP5zhN6WVXJ3R5ljGv.', 'ROLE_USER');

SELECT * FROM products;
```

In the application-postgres.properties:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/storedb
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.profiles.active=postgres

```

---

## 🚀 How to Run

```bash
# with H2 profile
SPRING_PROFILES_ACTIVE=h2 ./mvnw spring-boot:run

# With Postgres
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

---

## 📬 Contact

Built by **Razvan Nicolae** as part of a backend technical assignment.  
Feedback welcome!
