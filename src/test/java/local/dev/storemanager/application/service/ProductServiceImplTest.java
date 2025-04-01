package local.dev.storemanager.application.service;

import local.dev.storemanager.application.dto.ProductDto;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.application.service.product.ProductServiceImpl;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private ProductServiceImpl productService;

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

    @Test
    void findById_shouldReturnProduct_whenExists() {
        final var product = new Product(1L, "Laptop", 1200.0, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        final var found = productService.findById(1L);

        assertEquals("Laptop", found.getName());
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.findById(42L));
    }

    @Test
    void findAll_shouldReturnListOfProducts() {
        final var products = List.of(
                new Product(1L, "A", 1.0, 1),
                new Product(2L, "B", 2.0, 2)
        );

        when(productRepository.findAll()).thenReturn(products);

        final var result = productService.findAll();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getName());
    }

    @Test
    void updateProduct_shouldUpdateAndSave() {
        final var existing = Product.builder()
                .id(1L)
                .name("Old")
                .price(10.0)
                .quantity(2)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        final var dto = new ProductDto("New", 99.99, 10);
        final var updated = productService.updateProduct(1L, dto);

        assertEquals("New", updated.getName());
        assertEquals(99.99, updated.getPrice());
        assertEquals(10, updated.getQuantity());
        verify(productRepository).save(existing);
    }

    @Test
    void deleteProduct_shouldCallRepository() {
        productService.deleteProduct(42L);

        verify(productRepository).deleteById(42L);
    }
}