# 🛒 Store Management Microservice

A clean-architecture-based backend microservice built with **Java 17**, **Spring Boot**, **JWT security**, and **TDD-first** development practices.

This service manages products in a store and includes basic authentication with role-based access control. It is designed using Onion Architecture for high modularity and testability.

---

## ✅ Features Implemented

| Feature                            | Status | Notes                                      |
|-----------------------------------|--------|--------------------------------------------|
| Product entity & service          | ✅     | Built using TDD, tested with JUnit 5       |
| Onion architecture                | ✅     | Separate domain, application, infra layers |
| H2 local dev DB                   | ✅     | Runs in-memory with no setup required      |
| JWT authentication                | ✅     | Stateless, secure tokens via `jjwt`        |
| Role-based access control         | ✅     | `ROLE_ADMIN` and `ROLE_USER` supported     |
| Token-based login endpoint        | ✅     | `/auth/login` issues JWTs                  |

---

## 🧱 Project Architecture

This project follows a **Clean / Onion Architecture** style:

```
web.controller        --> Exposes REST APIs  
application.service   --> Implements business logic  
application.mapper    --> Maps DTOs ↔ Domain ↔ Entities  
domain.model          --> Pure models (no annotations)  
domain.repository     --> Abstract interfaces  
infrastructure.persistence --> JPA entities and adapters  
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

## 🧪 Project Setup & Product Service

### 🔹 Summary

- Bootstrapped Spring Boot project (Maven, Java 17)
- Designed base package structure for Onion Architecture
- Created `ProductDto`, `Product`, and `ProductEntity` models
- Built `ProductService.addProduct()` using **TDD first**
- Wrote unit test with mocked `ProductRepository` and `ProductMapper`
- Configured **H2 database** as in-memory dev DB

### 🔍 Sample Test (TDD-first)

```java
@Test
void addProduct_shouldConvertDtoAndSave() {
  final var dto = new ProductDto("Laptop", 19.99, 20);
  final var product = Product.builder()
          .name("Laptop")
          .price(19.99)
          .quantity(20)
          .build();

  when(productMapper.toDomain(dto)).thenReturn(product);

  productService.addProduct(dto);

  verify(productRepository).save(product);
}
```

---

## 🔐 JWT Authentication & Role-based Access

### 🔹 Summary

- Added `/auth/login` endpoint
- Integrated **JWT token generation** with `jjwt` library
- Tokens include username and role claims
- Added a security filter that:
    - Parses `Authorization` headers
    - Validates JWTs
    - Populates Spring Security context
- Restricted access to endpoints using roles
- Configured security with **stateless JWT** sessions

### 🧪 Login Example

```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}
```

Returns a JWT token.

### 🔐 Protected Access

To access `/products`, add the token to the request:

```http
Authorization: Bearer <token>
```

- Only `ROLE_ADMIN` can `POST /products`
- Any authenticated user can access protected routes

---

---

## ✅ Authentication Tests

### 🧪 `JwtUtilTest`

Unit tests for the `JwtUtil` class using a Spring Boot test context and a test-specific JWT secret defined in `application-test.properties`.

```java
@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

  @Autowired
  private JwtUtil jwtUtil;

  @Test
  void shouldGenerateAndValidateAdminToken() {
    final var token = jwtUtil.generateToken("adminUser", "ROLE_ADMIN");

    assertTrue(jwtUtil.validateToken(token));

    final var claims = jwtUtil.extractClaims(token);
    assertEquals("adminUser", claims.getSubject());
    assertEquals("ROLE_ADMIN", claims.get("role"));
  }

  @Test
  void shouldRejectTamperedToken() {
    final var token = jwtUtil.generateToken("user", "ROLE_USER");
    final var invalidToken = token + "garbage";
    assertFalse(jwtUtil.validateToken(invalidToken));
  }

  @Test
  void shouldRejectCompletelyInvalidToken() {
    assertFalse(jwtUtil.validateToken("this.is.not.valid"));
  }
}
```

- This test uses the `test` profile and automatically picks up `application-test.properties` for the JWT secret.
- Validates correct JWT generation and behavior on tampered/invalid tokens.

---

### 🧪 `AuthControllerTest`

Integration-style test using `@WebMvcTest` to test only the controller layer. Security is overridden using a test-specific `SecurityFilterChain` to avoid real authentication logic during testing.

```java
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @TestConfiguration
  static class JwtUtilTestConfig {
    @Bean
    public JwtUtil jwtUtil() {
      JwtUtil mock = Mockito.mock(JwtUtil.class);
      when(mock.generateToken("admin", "ROLE_ADMIN")).thenReturn("mocked-token");
      return mock;
    }

    @Bean
    public AuthenticationService authenticationService() {
      AuthenticationService mock = Mockito.mock(AuthenticationService.class);
      when(mock.resolveRole("admin", "admin")).thenReturn("ROLE_ADMIN");
      when(mock.resolveRole("wrong", "wrong")).thenReturn(null);
      return mock;
    }

    // Overriding security for this test because real Spring Security config is still active in the test,
    // and it's applying its full JWT logic even in @WebMvcTest
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http
              .csrf(AbstractHttpConfigurer::disable)
              .authorizeHttpRequests(auth -> auth
                      .anyRequest().permitAll()
              );
      return http.build();
    }
  }

  @Test
  void shouldReturnTokenForValidAdminLogin() throws Exception {
    mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                { "username": "admin", "password": "admin" }
                            """))
            .andExpect(status().isOk())
            .andExpect(content().string("mocked-token"));
  }

  @Test
  void shouldReturnUnauthorizedForInvalidLogin() throws Exception {
    mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                { "username": "wrong", "password": "wrong" }
                            """))
            .andExpect(status().isUnauthorized());
  }
}
```

- Uses `@WebMvcTest` for fast, controller-layer isolation
- Mocks `JwtUtil` and injects a dummy security config to bypass real authentication
- Confirms expected HTTP responses from the login endpoint

---


## ⚙️ Configuration: `application.properties`

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

## 🔜 Coming Next (Day 3+)

- Product CRUD endpoints
- Role-protected access
- Error handling & exception advice
- Caching (`@Cacheable`)
- Async Kafka integration for events

---

## 📬 Contact

Built by [Your Name] as part of a backend technical assignment.  
Feedback welcome!
