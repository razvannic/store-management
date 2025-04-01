# 🛒 Store Management Microservice

A clean-architecture-based backend microservice built with **Java 17**, **Spring Boot**, **JWT security**, and *
*TDD-first** development practices.

This service manages products in a store and includes basic authentication with role-based access control. It is
designed using Onion Architecture for high modularity and testability.

---

## ✅ Features Implemented

| Feature                        | Status | Notes                                      |
|--------------------------------|--------|--------------------------------------------|
| Product entity & service       | ✅      | Built using TDD, tested with JUnit 5       |
| Onion architecture             | ✅      | Separate domain, application, infra layers |
| H2 local dev DB                | ✅      | Runs in-memory with no setup required      |
| JWT authentication             | ✅      | Stateless, secure tokens via `jjwt`        |
| Role-based access control      | ✅      | `ROLE_ADMIN` and `ROLE_USER` supported     |
| Token-based login endpoint     | ✅      | `/auth/login` issues JWTs                  |
| Full CRUD with auth protection | ✅      | `POST`, `GET`, `PUT`, `DELETE`             |
| Product unit tests             | ✅      | Covers all public methods                  |
| Authentication unit tests      | ✅      | Covers authentication methods              |
| Integration unit tests         | ✅      | Covers product creation and authentication |

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
- Lombok
- JUnit 5 + Mockito

---

## 🧪 Product Service Coverage

### 🔹 Summary

- Bootstrapped with Maven & Java 17
- Created `ProductDto`, `Product`, and `ProductEntity`
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

## ✅ Authentication Tests

### 🔬 JwtUtilTest

```java

@Test
void shouldGenerateAndValidateAdminToken() {
  final var token = jwtUtil.generateToken("adminUser", "ROLE_ADMIN");

  assertTrue(jwtUtil.validateToken(token));

  final var claims = jwtUtil.extractClaims(token);
  assertEquals("adminUser", claims.getSubject());
  assertEquals("ROLE_ADMIN", claims.get("role"));
}
```

### 🔬 AuthControllerTest

```java

@Test
void shouldReturnTokenForValidAdminLogin() throws Exception {
  mockMvc.perform(post("/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                              { "username": "admin", "password": "admin" }
                          """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value("mocked-token"))
          .andExpect(jsonPath("$.tokenType").value("Bearer"))
          .andExpect(jsonPath("$.expiresIn").value(3600L));
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

## ⚙️ Configuration: application.properties

```properties
# H2 DB
spring.datasource.url=jdbc:h2:mem:storedb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
# JWT Secret
jwt.secret=kdiRmEFAxj2fnVqtu9nqx7BQox3Jr2VyNTOccDFRAK8=
```

---

## 🚀 How to Run

```bash
# Default profile: uses H2
./mvnw spring-boot:run

# With Postgres (if configured)
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

---

## 📬 Contact

Built by Razvan Nicolae as part of a backend technical assignment.  
Feedback welcome!
