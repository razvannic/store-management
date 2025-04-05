package local.dev.storemanager.application.service;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.exception.ProductNotFoundException;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.application.service.product.ProductServiceCacheableImpl;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class ProductServiceCacheableImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceCacheableImpl productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addProduct_shouldMapAndSave() {
        final var dto = new ProductRequestDto("Laptop", 999.99, 3);
        final var mapped = Product.builder().name("Laptop").price(999.99).quantity(3).build();

        when(productMapper.toDomain(dto)).thenReturn(mapped);
        when(productRepository.save(mapped)).thenReturn(mapped);

        final var result = productService.addProduct(dto);

        assertEquals("Laptop", result.getName());
        verify(productRepository).save(mapped);
    }

    @Test
    void findById_shouldReturnProduct() {
        final var product = Product.builder().id(1L).name("Tablet").price(499.99).quantity(5).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        final var result = productService.findById(1L);

        assertEquals("Tablet", result.getName());
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.findById(42L));
    }

    @Test
    void findAll_shouldReturnList() {
        final var products = List.of(
                new Product(1L, "A", 10.0, 1),
                new Product(2L, "B", 20.0, 2)
        );
        when(productRepository.findAll()).thenReturn(products);

        final var result = productService.findAll();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
    }

    @Test
    void updateProduct_shouldModifyAndSave() {
        final var existing = Product.builder().id(1L).name("Old").price(10.0).quantity(1).build();
        final var dto = new ProductRequestDto("New", 99.99, 9);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        final var result = productService.updateProduct(1L, dto);

        assertEquals("New", result.getName());
        assertEquals(99.99, result.getPrice());
        assertEquals(9, result.getQuantity());
        verify(productRepository).save(existing);
    }

    @Test
    void deleteProduct_shouldCallRepository() {
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }
}
