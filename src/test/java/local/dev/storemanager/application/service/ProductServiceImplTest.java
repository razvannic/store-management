package local.dev.storemanager.application.service;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.application.service.product.ProductServiceImpl;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

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
        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cacheManager.getCache("products")).thenReturn(cache);

        productService.addProduct(dto);

        verify(productRepository).save(product);
        verify(cache).put(any(), eq(product));
        // for "products"
        verify(cache).clear();
    }

    @Test
    void findById_shouldReturnFromRepository_ifNotCached() {
        var product = new Product(1L, "Laptop", 1200.0, 5);
        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cache.get(1L, Product.class)).thenReturn(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var result = productService.findById(1L);

        assertEquals("Laptop", result.getName());
        verify(cache).put(1L, product);
    }

    @Test
    void findById_shouldReturnFromCache_ifCached() {
        var cached = new Product(1L, "CachedProduct", 100.0, 2);
        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cache.get(1L, Product.class)).thenReturn(cached);

        var result = productService.findById(1L);

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
        var list = List.of(
                new Product(1L, "One", 10.0, 1),
                new Product(2L, "Two", 20.0, 2)
        );

        when(cacheManager.getCache("products")).thenReturn(cache);
        when(cache.get("all", List.class)).thenReturn(null);
        when(productRepository.findAll()).thenReturn(list);

        var result = productService.findAll();

        assertEquals(2, result.size());
        verify(cache).put("all", list);
    }

    @Test
    void updateProduct_shouldSaveAndRefreshCache() {
        var existing = Product.builder().id(1L).name("Old").price(10.0).quantity(2).build();
        var dto = new ProductRequestDto("New", 99.99, 10);

        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cache.get(1L, Product.class)).thenReturn(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(cacheManager.getCache("products")).thenReturn(cache);

        var updated = productService.updateProduct(1L, dto);

        assertEquals("New", updated.getName());
        assertEquals(99.99, updated.getPrice());
        assertEquals(10, updated.getQuantity());

        verify(cache, times(2)).put(eq(1L), any(Product.class));

        verify(cache).clear();
    }

    @Test
    void deleteProduct_shouldEvictFromCaches() {
        when(cacheManager.getCache("product")).thenReturn(cache);
        when(cacheManager.getCache("products")).thenReturn(cache);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
        verify(cache).evict(1L);
        verify(cache).clear();
    }
}
