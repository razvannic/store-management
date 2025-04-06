package local.dev.storemanager.integration;

import static local.dev.storemanager.config.KafkaTopics.PRODUCTS_TOPIC;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.domain.model.product.ImportMode;
import local.dev.storemanager.domain.service.ProductBulkImportService;
import local.dev.storemanager.infrastructure.persistence.config.PostgresTestContainer;
import local.dev.storemanager.infrastructure.persistence.jparepository.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = PRODUCTS_TOPIC)
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class ProductBulkImportServiceIntegrationTest {


    @Autowired
    private ProductBulkImportService bulkImportService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductJpaRepository productJpaRepository;


    @BeforeEach
    void setup() {
        productJpaRepository.deleteAll();
    }

    @Test
    void shouldImportProductsSingleThreaded() throws Exception {
        final var products = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            products.add(new ProductRequestDto("Product-" + i, 10.0 + i, i + 1,
                    null, null, null, null, null, null, null));
        }

        final var json = objectMapper.writeValueAsBytes(products);
        final var file = new MockMultipartFile("file", "products.json", "application/json", json);

        final var result = bulkImportService.importFromJson(file, ImportMode.SINGLE_THREADED);

        assertEquals(500, result.total());
        assertEquals(500, result.success());
        assertEquals(0, result.failed());
    }

    @Test
    void shouldImportProductsMultiThreaded() throws Exception {
        final var products = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            products.add(new ProductRequestDto("Product-" + i, 10.0 + i, i + 1,
                    null, null, null, null, null, null, null));
        }

        final var json = objectMapper.writeValueAsBytes(products);
        final var file = new MockMultipartFile("file", "products.json", "application/json", json);

        final var result = bulkImportService.importFromJson(file, ImportMode.MULTI_THREADED);

        assertEquals(500, result.total());
        assertEquals(500, result.success());
        assertEquals(0, result.failed());
    }

}
