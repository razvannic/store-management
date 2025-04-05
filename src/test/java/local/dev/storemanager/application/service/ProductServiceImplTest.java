package local.dev.storemanager.application.service;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.kafka.ProductEventPublisher;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.application.service.product.ProductServiceImpl;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static local.dev.storemanager.config.CacheNames.PRODUCT;
import static local.dev.storemanager.config.CacheNames.PRODUCTS;
import static local.dev.storemanager.constants.EventTypes.PRICE_CHANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private ProductEventPublisher productEventPublisher;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void addProduct_shouldConvertDtoAndSave() {
        var dto = new ProductRequestDto("Laptop", 19.99, 20);
        var product = Product.builder()
                .name("Laptop")
                .price(19.99)
                .quantity(20)
                .build();

        when(productMapper.toDomain(dto)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(cacheManager.getCache(PRODUCT)).thenReturn(cache);
        when(cacheManager.getCache(PRODUCTS)).thenReturn(cache);

        productService.addProduct(dto);

        verify(productRepository).save(product);
        verify(cache).put(any(), eq(product));
        // for "products"
        verify(cache).clear();
    }

    @Test
    void findById_shouldReturnFromRepository_ifNotCached() {
        final var product = new Product(1L, "Laptop", 1200.0, 5);
        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cache.get(1L, Product.class)).thenReturn(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        final var result = productService.findById(1L);

        assertEquals("Laptop", result.getName());
        verify(cache).put(1L, product);
    }

    @Test
    void findById_shouldReturnFromCache_ifCached() {
        final var cached = new Product(1L, "CachedProduct", 100.0, 2);
        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cache.get(1L, Product.class)).thenReturn(cached);

        final var result = productService.findById(1L);

        assertEquals("CachedProduct", result.getName());
        verify(productRepository, never()).findById(anyLong());
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cache.get(1L, Product.class)).thenReturn(null);
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.findById(1L));
    }

    @Test
    void findAll_shouldReturnFromRepository_andCache() {
        final var list = List.of(
                new Product(1L, "One", 10.0, 1),
                new Product(2L, "Two", 20.0, 2)
        );

        when(cacheManager.getCache(PRODUCTS)).thenReturn(cache);
        when(cache.get("all", List.class)).thenReturn(null);
        when(productRepository.findAll()).thenReturn(list);

        final var result = productService.findAll();

        assertEquals(2, result.size());
        verify(cache).put("all", list);
    }

    @Test
    void updateProduct_shouldSaveAndRefreshCache() {
        final var existing = Product.builder().id(1L).name("Old").price(10.0).quantity(2).build();
        final var dto = new ProductRequestDto("New", 99.99, 10);

        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cache.get(1L, Product.class)).thenReturn(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(cacheManager.getCache(PRODUCTS)).thenReturn(cache);

        final var updated = productService.updateProduct(1L, dto);

        assertEquals("New", updated.getName());
        assertEquals(99.99, updated.getPrice());
        assertEquals(10, updated.getQuantity());

        verify(cache, times(2)).put(eq(1L), any(Product.class));

        verify(cache).clear();
    }

    @Test
    void updateProduct_shouldPublishPriceChangeEvent() {
        when(cacheManager.getCache(PRODUCT)).thenReturn(cache);
        when(cacheManager.getCache(PRODUCTS)).thenReturn(cache);

        final var existing = Product.builder()
                .id(1L)
                .name("Book")
                .price(10.0)
                .quantity(3)
                .build();

        final var updated = Product.builder()
                .id(1L)
                .name("Book")
                .price(15.0)
                .quantity(3)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(updated);
        when(cache.get(1L, Product.class)).thenReturn(null); // simulate cache miss

        final var dto = new ProductRequestDto("Book", 15.0, 3);

        productService.updateProduct(1L, dto);

        verify(productRepository).save(existing);
        verify(productEventPublisher).publish(eq(PRODUCTS), argThat(event ->
                PRICE_CHANGED.equals(event.getType()) &&
                        String.valueOf(existing.getId()).equals(event.getPayload())
        ));
    }

    @Test
    void updateProduct_shouldNotPublishEventWhenPriceUnchanged() {
        when(cacheManager.getCache(PRODUCT)).thenReturn(cache);
        when(cacheManager.getCache(PRODUCTS)).thenReturn(cache);

        final var existing = Product.builder()
                .id(1L)
                .name("Book")
                .price(10.0)
                .quantity(3)
                .build();

        final var updated = Product.builder()
                .id(1L)
                .name("Book")
                .price(10.0)
                .quantity(3)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(updated);
        // simulating cache miss
        when(cache.get(1L, Product.class)).thenReturn(null);

        final var dto = new ProductRequestDto("Book", 10.0, 3);

        productService.updateProduct(1L, dto);

        verify(productEventPublisher, never()).publish(any(), any());
    }

    @Test
    void deleteProduct_shouldEvictFromCaches() {
        when(cacheManager.getCache(PRODUCT)).thenReturn(cache);
        when(cacheManager.getCache(PRODUCTS)).thenReturn(cache);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
        verify(cache).evict(1L);
        verify(cache).clear();
    }
}
