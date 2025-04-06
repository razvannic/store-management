# 🏍️ Store Management Microservice

A clean-architecture-based backend microservice built with **Java 17**, **Spring Boot**, **JWT security**, and *
*TDD-first** development practices.

This service manages products in a store and includes basic authentication with role-based access control. It is
designed using Onion Architecture for high modularity and testability.

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
infrastructure.kafka        --> Kafka event publishing/listening  
infrastructure.metrics      --> Prometheus metric counter setup  
```

### 🔍 About Onion Architecture

The Onion Architecture separates code into concentric layers:

- **Domain**: The innermost core — business models and logic, completely framework-agnostic.
- **Application**: Use cases and service logic — depends only on domain abstractions.
- **Infrastructure**: Implements external systems (DB, Kafka, etc.), depends on domain and application.
- **Web (Controller)**: Entry point to the system (HTTP), wires everything together.

**Advantages:**

- Testability: Core logic is isolated from external concerns.
- Maintainability: Each layer has clear responsibilities.
- Flexibility: Infrastructure can be swapped without touching business logic.

---

## ✅ Features Implemented

| Feature                                | Status | Notes                                                   |
|----------------------------------------|--------|---------------------------------------------------------|
| Product entity & service               | ✅      | Built using TDD, tested with JUnit 5                    |
| Onion architecture                     | ✅      | Separate domain, application, infra layers              |
| H2 local dev DB                        | ✅      | Runs in-memory with no setup required                   |
| PostgreSQL support with Testcontainers | ✅      | Integration tests run with Postgres + JSONB support     |
| JWT authentication                     | ✅      | Stateless, secure tokens via `jjwt`                     |
| Role-based access control              | ✅      | `ROLE_ADMIN` and `ROLE_USER` supported                  |
| Token-based login endpoint             | ✅      | `/auth/login` issues JWTs                               |
| Full CRUD with auth protection         | ✅      | `POST`, `GET`, `PUT`, `DELETE`                          |
| Product unit tests                     | ✅      | Covers all public methods                               |
| Authentication unit tests              | ✅      | Covers authentication methods                           |
| Integration unit tests                 | ✅      | Covers product creation and authentication              |
| Input validation with Jakarta          | ✅      | `@NotBlank`, `@Min`, etc. on DTO fields                 |
| Global exception handling              | ✅      | Custom `@ControllerAdvice` implementation               |
| Configurable token expiration          | ✅      | via `application.properties`                            |
| Caching with Spring & Caffeine         | ✅      | `findById`, `update`, `delete` optimized                |
| Product Type Sealed Interfaces         | ✅      | Java 17 sealed types like `Book`, `Clothing`, etc.      |
| Product Filtering                      | ✅      | By `type`, `author`, `brand`, or `size` via query param |
| JSONB column for dynamic fields        | ✅      | `ProductType` stored as JSONB in Postgres               |
| Kafka Integration                      | ✅      | Publishes product events like CREATED, UPDATED          |
| Embedded Kafka Integration Test        | ✅      | Publishes and consumes events with real broker          |
| Bulk product import via JSON file      | ✅      | Multipart `/products/import` endpoint                   |
| Multi-threaded bulk import             | ✅      | Parallel batches with configurable size                 |
| Prometheus Metrics + Micrometer        | ✅      | Exposes `product_events_published_total{...}` counter   |
| Swagger / OpenAPI documentation        | ✅      | Auto-generated API docs via springdoc-openapi           |
| Postman collection                     | ✅      | Available in project root `/postman/`                   |

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
| `findAll()`       | Returns all products                  |
| `updateProduct()` | Validates update + save               |
| `deleteProduct()` | Verifies `deleteById()` called        |

---

## 🔁 Integration Testing

### 🔬 ProductIntegrationTest

```java

@Test
void shouldUpdateProductAsAdmin() throws Exception {
    final var saved = productJpaRepository.save(
            new ProductEntity(null, "Java 8", 15.0, 10, new Book("John Smith", "Programming"))
    );

    final var token = getAuthToken("admin", "admin");

    mockMvc.perform(put("/products/" + saved.getId())
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                    "name": "Modern Java",
                                    "price": 29.99,
                                    "quantity": 8,
                                    "type": "Book",
                                    "author": "Venkat Subramaniam",
                                    "genre": "Technology"
                                }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Modern Java"))
            .andExpect(jsonPath("$.price").value(29.99))
            .andExpect(jsonPath("$.quantity").value(8))
            .andExpect(jsonPath("$.typeDetails.author").value("Venkat Subramaniam"))
            .andExpect(jsonPath("$.typeDetails.genre").value("Technology"));
}
```

- Uses real JWT authentication and Postgres DB
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
        //...
) {
}
```

On error:

```json
{
  "status": 400,
  "error": "Bad Request",
  "messages": [
    {
      "field": "price",
      "message": "Price must be greater than 0"
    },
    ...
  ]
}
```

---

## ⚙️ Configuration: application.properties

```properties
spring.application.name=store-manager
server.port=8099
# base64-encoded 256+ bit secret
jwt.secret=VGhpcy1zaG91bGQtYmUtYXRsZWFzdC0zMi1ieXRlcy1sb25nLWxvbmcKVGhpcy1pcw==
jwt.expiration:3600000
# ================================
# caching
# ================================
spring.cache.type=caffeine
spring.cache.cache-names=product,products
# max 100 entries, expiring 10 seconds after write
spring.cache.caffeine.spec=maximumSize=100,expireAfterWrite=10s
# Logging levels
logging.level.root=INFO
logging.level.local.dev.storemanager.application=DEBUG
# Swagger info
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
# Kafka
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.group-id=store-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.properties.spring.json.trusted.packages=*
# Metrics
management.endpoints.web.exposure.include=*
management.endpoint.prometheus.access=read_only
management.prometheus.metrics.export.enabled=true

```

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
createdb
store_db
```

Check it works:

```sql
psql
store_db
```

Then run a sample query:

```sql
\dt       -- see tables

-- Create user
CREATE
USER store_user WITH PASSWORD 'store_pass';

-- Create database
CREATE
DATABASE store_db OWNER store_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE
store_db TO store_user;

\c
store_db store_user

CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL
);

INSERT INTO users (username, password, role)
VALUES ('admin', '$2a$10$9rd8UwWOqE1OGk3THNmlouOeNlf41sScbrpsKwEtEz/OL6AcZrBry', 'ROLE_ADMIN'),
       ('user', '$2a$10$vVQBCU7JSWLE6whk88AyQ.wTEtnEeSsZr9DP5zhN6WVXJ3R5ljGv.', 'ROLE_USER');

CREATE TABLE products
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100),
    price DOUBLE,
    quantity INT,
    type     JSON
);
```

In the application-postgres.properties:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/store_db
spring.datasource.username=store_user
spring.datasource.password=store_pass
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.h2.console.enabled=false


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

### 🧪 Manual Cache Access Example

```java
Cache productCache = cacheManager.getCache("product");
Product cached = productCache.get(productId, Product.class);
if(cached !=null)return cached;
```

### 🧪 Cache Put and Evict

```java
productCache.put(productId, product); // on save/update
productCache.

evict(productId);        // on delete
```

---

## 📦 Bulk Product Import

Upload JSON file to create products in bulk:

### Example

```http
POST /products/import
Content-Type: multipart/form-data

file: products.json
```

`products.json`

```json
[
  {
    "name": "Laptop",
    "price": 999.99,
    "quantity": 5,
    "type": "Electronics",
    "brand": "Dell",
    "warranty": "2 years"
  },
  {
    "name": "T-shirt",
    "price": 19.99,
    "quantity": 15,
    "type": "Clothing",
    "size": "M",
    "material": "Cotton"
  }
]
```

---

## 📥 Bulk Product Import (JSON Upload)

The application supports uploading thousands of products using a JSON file.

### 🔹 Endpoint
`POST /products/bulk/import`
- Accepts `multipart/form-data`
- File must be valid JSON array of product objects
- Accepts query param `mode=SINGLE_THREADED|MULTI_THREADED`

### 🔹 Processing Modes
- **SINGLE_THREADED**: sequential processing
- **MULTI_THREADED**: uses a thread pool and splits input into batches
- Batch size is configurable via:

### 🔹 Performance Comparison (for 2000 products per file)
| Mode             | Products | Duration (ms) |
|------------------|----------|---------------|
| SINGLE_THREADED  | 2000     | 3354          |
| MULTI_THREADED   | 2000     | 1317          |

> 💡 Multi-threaded import improves performance significantly when dealing with large files.

```properties
import.bulk.batch-size=100
```

### 🔹 Sample JSON File
```json
[
  {
    "name": "Monitor",
    "price": 129.99,
    "quantity": 10,
    "type": "Electronics",
    "brand": "LG",
    "warranty": "2 years"
  },
  {
    "name": "Clean Code",
    "price": 39.99,
    "quantity": 5,
    "type": "Book",
    "author": "Robert C. Martin",
    "genre": "Software"
  }
]
```

### 🧪 Testing Strategy
- **Integration unit tests** use real DB and Kafka to verify full processing and cover both single and multi-threaded logic
- Postman collections provided for real-world testing of large imports (500–2000+ products)
- Product files found in /postman/bulk-products (should be dropped in postman form-data body)

---

## 📊 Metrics with Micrometer & Prometheus

The application includes Micrometer-based metrics to track Kafka publishing activity.

### 🔸 Available Metric

A custom counter is defined to track published Kafka events:

```
product_events_published_total{eventType="PRODUCT_CREATED",topic="products"} 4.0
```

### 🛠️ Configuration

Add this to `application.properties` to expose metrics:

```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true
```

Access the metrics at:

```
GET http://localhost:8099/actuator/prometheus
```

### ✅ Test Coverage Example

Metrics are tested using `SimpleMeterRegistry` in integration tests:

```java
SimpleMeterRegistry registry = new SimpleMeterRegistry();
ProductEventPublisher publisher = new ProductEventPublisher(kafkaTemplate, registry);

publisher.

publish("products",new ProductEvent("PRODUCT_CREATED", "payload"));

Counter counter = registry.find("product_events_published_total")
        .tags("eventType", "PRODUCT_CREATED", "topic", "products")
        .counter();

assertNotNull(counter);

assertEquals(1.0,counter.count());
```

---

## 🚀 Bulk Product Initialization

The app provides a POST endpoint for uploading a JSON file containing multiple products:

```http
POST /products/bulk
Content-Type: multipart/form-data
```

The file must be a JSON array of `ProductRequestDto` entries:

```json
[
  {
    "name": "Java Book",
    "price": 29.99,
    "quantity": 10,
    "type": "Book",
    "author": "Joshua Bloch",
    "genre": "Programming"
  },
  {
    "name": "Monitor",
    "price": 199.99,
    "quantity": 5,
    "type": "Electronics",
    "brand": "Dell",
    "warranty": "2 years"
  }
]
```

### Test Example

```java

@Test
void shouldUploadBulkProducts() throws Exception {
    final var token = getAuthToken("admin", "admin");
    var file = new ClassPathResource("products-bulk.json");

    mockMvc.perform(multipart("/products/bulk")
                    .file("file", file.getContentAsByteArray())
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated());
}
```

---

## 📉 Kafka Event Publishing

Kafka topic: `products`

When a product is created or updated (price change), a `ProductEvent` is published:

```json
{
  "type": "PRODUCT_CREATED",
  "payload": "productId"
}
```

**Tested with Embedded Kafka**:

```java
final var event = new ProductEvent(PRODUCT_CREATED, UUID.randomUUID().toString());
publisher.

publish("products",event);

var record = KafkaTestUtils.getRecords(consumer).iterator().next();

assertEquals(PRODUCT_CREATED, record.value().

getType());
```

---

## 🚚 Product Filtering via Query Params

Supports query params:

- `type=Book`
- `author=Joshua Bloch`
- `brand=Dell`
- `size=M`

Example:

```
GET /products?type=Book&author=Joshua Bloch
```

Filters using Java 17 sealed interfaces under the hood.

---

## ⚙️ Configuration Profiles

Profiles:

- `h2` → in-memory dev DB
- `postgres` → real DB with JSONB
- `test` → used with Testcontainers

To run with Postgres:

```bash
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

---

## 📦 Docker & Compose

To run with Docker Compose:

```bash
docker compose up --build
```

Includes:

- `store-app`
- `store-postgres`
- `kafka`
- `zookeeper`
- `kafka-ui`

---

## 📁 Swagger Integration

API documentation is automatically generated using **springdoc-openapi**.

- Access it locally:

```
http://localhost:8099/swagger-ui.html
```

### ⚡ Benefits:

- Interactive documentation
- Try out endpoints directly from the browser
- Useful for frontend devs and API consumers

No extra configuration is needed—springdoc scans your `@RestController` methods and builds the documentation
automatically.

---

## 📂 Postman Collection

A complete **Postman collection** is included in the project to test endpoints quickly:

**Path**: `postman/store-manager.postman_collection.json`

### ☑️ Collection Includes:

- Auth request with login
- Protected product CRUD requests with bearer token
- Import product JSON
- Invalid input tests

### ❇ Tip:

Use environment variables in Postman to store the JWT:

```json
{{{{
  product_token
}}
}
}
```

Import the collection via Postman UI > *Import* > *File Upload*.

---

## 🌟 Author

Built by **Razvan Nicolae** as part of backend technical assignment.

Feedback and suggestions welcome!

