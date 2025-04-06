package local.dev.storemanager.application.service.product;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.domain.model.product.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.domain.service.ProductService;
import local.dev.storemanager.infrastructure.persistence.config.PostgresTestContainer;
import local.dev.storemanager.infrastructure.persistence.jparepository.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static local.dev.storemanager.config.CacheNames.PRODUCT;
import static local.dev.storemanager.config.CacheNames.PRODUCTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = { "products" })
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class ProductServiceCachingTest {

    @Autowired
    @Qualifier("productServiceImpl")
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanUp() {
        productJpaRepository.deleteAll();
        cacheManager.getCache(PRODUCT).clear();
        cacheManager.getCache(PRODUCTS).clear();
    }

    @Test
    void shouldUseCacheInFindById() {
        final var saved = productRepository.save(new Product(null, "Monitor", 150.0, 5, null));

        // First call — hits DB and populates cache
        final var first = productService.findById(saved.getId());
        assertEquals("Monitor", first.getName());

        // Delete from DB
        productRepository.deleteById(saved.getId());

        // Second call — should return cached result
        final var second = productService.findById(saved.getId());
        assertNotNull(second);
        assertEquals("Monitor", second.getName());
    }

    @Test
    void shouldUpdateCacheAfterUpdate() {
        final var saved = productRepository.save(new Product(null, "Mouse", 20.0, 4, null));

        // Load into cache
        productService.findById(saved.getId());

        // Update
        final var updated = new ProductRequestDto("Wireless Mouse", 25.0, 8,null, null , null,
                null, null, null, null);
        productService.updateProduct(saved.getId(), updated);

        // Cached value should reflect the update
        final var fromCache = cacheManager.getCache("product").get(saved.getId(), Product.class);
        assertNotNull(fromCache);
        assertEquals("Wireless Mouse", fromCache.getName());
        assertEquals(25.0, fromCache.getPrice());

        // "products" cache should be cleared
        assertNull(cacheManager.getCache(PRODUCTS).get("all"));
    }

    @Test
    void shouldEvictCacheOnDelete() {
        final var saved = productRepository.save(new Product(null, "Keyboard", 49.0, 10, null));

        // Load into cache
        productService.findById(saved.getId());

        // Delete
        productService.deleteProduct(saved.getId());

        // Cache for individual product should be gone
        assertNull(cacheManager.getCache("product").get(saved.getId(), Product.class));

        // Cache for list of products should also be evicted
        assertNull(cacheManager.getCache(PRODUCTS).get("all"));
    }
}
