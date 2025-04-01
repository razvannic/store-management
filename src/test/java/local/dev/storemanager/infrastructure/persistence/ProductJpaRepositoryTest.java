package local.dev.storemanager.infrastructure.persistence;

import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductJpaRepositoryTest {
    @Autowired
    private ProductJpaRepository jpaRepository;

    @Test
    @DisplayName("should save and retrieve a product")
    void shouldSaveAndRetrieveProduct() {
        final var product = new ProductEntity(null, "Book", 15.5, 10);
        final var saved = jpaRepository.save(product);

        final var found = jpaRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Book", found.get().getName());
        assertEquals(15.5, found.get().getPrice());
        assertEquals(10, found.get().getQuantity());
    }

    @Test
    @DisplayName("should delete a product")
    void shouldDeleteProduct() {
        final var product = new ProductEntity(null, "Pen", 2.0, 100);
        final var saved = jpaRepository.save(product);

        jpaRepository.deleteById(saved.getId());

        final var found = jpaRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}