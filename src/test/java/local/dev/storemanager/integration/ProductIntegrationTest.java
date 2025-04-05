package local.dev.storemanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import local.dev.storemanager.domain.model.product.Book;
import local.dev.storemanager.domain.model.product.Electronics;
import local.dev.storemanager.infrastructure.persistence.config.PostgresTestContainer;
import local.dev.storemanager.infrastructure.persistence.entity.AppUser;
import local.dev.storemanager.infrastructure.persistence.jparepository.ProductJpaRepository;
import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import local.dev.storemanager.infrastructure.persistence.jparepository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
@EmbeddedKafka(partitions = 1, topics = {"products"})
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        productJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        userJpaRepository.save(new AppUser(null, "admin", passwordEncoder.encode("admin"), "ROLE_ADMIN"));
        userJpaRepository.save(new AppUser(null, "user", passwordEncoder.encode("user"), "ROLE_USER"));
    }

    private String getAuthToken(String username, String password) throws Exception {
        final var requestBody = String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                """, username, password);

        final var responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("accessToken").asText();
    }

    @Test
    void shouldCreateProductAsAdmin() throws Exception {
        final var token = getAuthToken("admin", "admin");

        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {
                                        "name": "Java in Action",
                                        "price": 39.99,
                                        "quantity": 10,
                                        "type": "Book",
                                        "author": "Raoul-Gabriel Urma",
                                        "genre": "Programming"
                                    }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java in Action"))
                .andExpect(jsonPath("$.type").value("Book"))
                .andExpect(jsonPath("$.typeDetails.author").value("Raoul-Gabriel Urma"))
                .andExpect(jsonPath("$.typeDetails.genre").value("Programming"));
    }

    @Test
    void shouldGetAllProductsAsUser() throws Exception {
        productJpaRepository.save(new ProductEntity(null, "Surface Pro", 1199.0, 5, null));
        productJpaRepository.save(new ProductEntity(null, "Logitech MX", 89.0, 30, null));
        productJpaRepository.save(
                new ProductEntity(null, "Surface Pro", 1199.0, 5,
                        new Electronics("Microsoft", "1 year"))
        );
        productJpaRepository.save(
                new ProductEntity(null, "Logitech MX", 89.0, 30,
                        new Electronics("Logi", "2 years"))
        );


        final var token = getAuthToken("user", "user");

        mockMvc.perform(get("/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(4)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    void shouldFilterWhenSomeProductsHaveNullType() throws Exception {
        productJpaRepository.save(new ProductEntity(null, "Java Book", 25.0, 5,
                new Book("Joshua Bloch", "Programming")));
        productJpaRepository.save(new ProductEntity(null, "Notebook", 3.0, 20, null));

        final var token = getAuthToken("user", "user");

        mockMvc.perform(get("/products?type=Book")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Java Book"));
    }


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

    @Test
    void shouldDeleteProductAsAdmin() throws Exception {
        final var saved = productJpaRepository.save(new ProductEntity(null, "Desk Lamp", 35.0, 8, null));
        final var token = getAuthToken("admin", "admin");

        mockMvc.perform(delete("/products/" + saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/" + saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product with id " + saved.getId() + " was not found."));
    }

    @Test
    void shouldRejectUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isForbidden());
    }

}
