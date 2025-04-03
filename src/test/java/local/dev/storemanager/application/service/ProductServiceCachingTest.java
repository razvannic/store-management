package local.dev.storemanager.application.service;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.domain.service.ProductService;
import local.dev.storemanager.infrastructure.persistence.jparepository.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
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
        cacheManager.getCache("product").clear();
        cacheManager.getCache("products").clear();
    }

    @Test
    void shouldUseCacheInFindById() {
        Product saved = productRepository.save(new Product(null, "Monitor", 150.0, 5));

        // First call — hits DB and populates cache
        Product first = productService.findById(saved.getId());
        assertEquals("Monitor", first.getName());

        // Delete from DB
        productRepository.deleteById(saved.getId());

        // Second call — should return cached result
        Product second = productService.findById(saved.getId());
        assertNotNull(second);
        assertEquals("Monitor", second.getName());
    }

    @Test
    void shouldUpdateCacheAfterUpdate() {
        Product saved = productRepository.save(new Product(null, "Mouse", 20.0, 4));

        // Load into cache
        productService.findById(saved.getId());

        // Update
        ProductRequestDto updated = new ProductRequestDto("Wireless Mouse", 25.0, 8);
        productService.updateProduct(saved.getId(), updated);

        // Cached value should reflect the update
        Product fromCache = cacheManager.getCache("product").get(saved.getId(), Product.class);
        assertNotNull(fromCache);
        assertEquals("Wireless Mouse", fromCache.getName());
        assertEquals(25.0, fromCache.getPrice());

        // "products" cache should be cleared
        assertNull(cacheManager.getCache("products").get("all"));
    }

    @Test
    void shouldEvictCacheOnDelete() {
        Product saved = productRepository.save(new Product(null, "Keyboard", 49.0, 10));

        // Load into cache
        productService.findById(saved.getId());

        // Delete
        productService.deleteProduct(saved.getId());

        // Cache for individual product should be gone
        assertNull(cacheManager.getCache("product").get(saved.getId(), Product.class));

        // Cache for list of products should also be evicted
        assertNull(cacheManager.getCache("products").get("all"));
    }
}
