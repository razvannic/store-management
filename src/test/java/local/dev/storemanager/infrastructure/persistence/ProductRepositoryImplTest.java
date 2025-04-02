package local.dev.storemanager.infrastructure.persistence;

import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductRepositoryImplTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveAndFindProduct() {
        final var toSave = Product.builder().name("Notebook").price(5.0).quantity(3).build();

        final var saved = productRepository.save(toSave);

        Optional<Product> found = productRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Notebook", found.get().getName());
    }

    @Test
    void shouldReturnAllProducts() {
        productRepository.save(Product.builder().name("Pencil").price(1.0).quantity(100).build());
        productRepository.save(Product.builder().name("Eraser").price(0.5).quantity(50).build());

        List<Product> products = productRepository.findAll();
        assertTrue(products.size() >= 2);
    }

    @Test
    void shouldDeleteProduct() {
        final var saved = productRepository.save(Product.builder().name("Pen").price(2.5).quantity(10).build());
        productRepository.deleteById(saved.getId());

        final var found = productRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}