package local.dev.storemanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import local.dev.storemanager.infrastructure.persistence.entity.AppUser;
import local.dev.storemanager.infrastructure.persistence.jparepository.ProductJpaRepository;
import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import local.dev.storemanager.infrastructure.persistence.jparepository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = { "products" })
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
                                        "name": "Keyboard",
                                        "price": 49.99,
                                        "quantity": 20
                                    }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void shouldGetAllProductsAsUser() throws Exception {
        productJpaRepository.save(new ProductEntity(null, "Notebook", 4.5, 15));
        productJpaRepository.save(new ProductEntity(null, "Pen", 1.5, 100));

        final var token = getAuthToken("user", "user");

        mockMvc.perform(get("/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    void shouldUpdateProductAsAdmin() throws Exception {
        final var saved = productJpaRepository.save(
                new ProductEntity(null, "Mouse", 15.0, 10)
        );

        final var token = getAuthToken("admin", "admin");

        mockMvc.perform(put("/products/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {
                                        "name": "Wireless Mouse",
                                        "price": 20.0,
                                        "quantity": 5
                                    }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.price").value(20.0))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void shouldDeleteProductAsAdmin() throws Exception {
        final var saved = productJpaRepository.save(new ProductEntity(null, "Desk Lamp", 35.0, 8));
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
