package local.dev.storemanager.infrastructure.persistence;

import local.dev.storemanager.domain.model.product.Book;
import local.dev.storemanager.domain.model.product.Clothing;
import local.dev.storemanager.domain.model.product.Electronics;
import local.dev.storemanager.domain.model.product.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.infrastructure.persistence.config.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class ProductRepositoryImplTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveAndFindProduct() {
        final var toSave = Product.builder()
                .name("Clean Code")
                .price(35.0)
                .quantity(10)
                .type(new Book("Robert C. Martin", "Programming"))
                .build();

        final var saved = productRepository.save(toSave);

        Optional<Product> found = productRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Clean Code", found.get().getName());
        assertTrue(found.get().getType() instanceof Book);
    }

    @Test
    void shouldReturnAllProducts() {
        productRepository.save(Product.builder()
                .name("MacBook Pro")
                .price(1999.0)
                .quantity(5)
                .type(new Electronics("Apple", "2 years"))
                .build());

        productRepository.save(Product.builder()
                .name("T-Shirt")
                .price(19.99)
                .quantity(50)
                .type(new Clothing("M", "Cotton"))
                .build());

        final var products = productRepository.findAll();
        assertTrue(products.size() >= 2);
    }

    @Test
    void shouldDeleteProduct() {
        final var saved = productRepository.save(Product.builder()
                .name("Mechanical Keyboard")
                .price(129.99)
                .quantity(8)
                .type(new Electronics("Keychron", "1 year"))
                .build());

        productRepository.deleteById(saved.getId());

        final var found = productRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}
