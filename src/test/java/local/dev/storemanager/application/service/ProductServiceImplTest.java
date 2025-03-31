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
}