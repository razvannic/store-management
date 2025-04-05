package local.dev.storemanager.infrastructure.persistence;

import local.dev.storemanager.domain.model.product.Book;
import local.dev.storemanager.infrastructure.persistence.config.PostgresTestContainer;
import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import local.dev.storemanager.infrastructure.persistence.jparepository.ProductJpaRepository;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class ProductJpaRepositoryTest {
    @Autowired
    private ProductJpaRepository jpaRepository;

    @Test
    @DisplayName("should save and retrieve a product")
    void shouldSaveAndRetrieveProduct() {
        final var product = new ProductEntity(null, "Book", 15.5, 10, null);
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
        final var product = new ProductEntity(null, "Pen", 2.0, 100, null);
        final var saved = jpaRepository.save(product);

        jpaRepository.deleteById(saved.getId());

        final var found = jpaRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("should save and retrieve product with null type")
    void shouldSaveAndRetrieveProductWithNullType() {
        final var product = new ProductEntity(
                null,
                "Generic Notebook",
                3.0,
                50,
                null
        );

        final var saved = jpaRepository.save(product);
        final var found = jpaRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Generic Notebook", found.get().getName());
        assertNull(found.get().getType());
    }

    @Test
    @DisplayName("should correctly persist and load complex Book type")
    void shouldHandleComplexBookType() {
        final var product = new ProductEntity(
                null,
                "Clean Architecture",
                55.0,
                7,
                new Book("Robert C. Martin", "Software Engineering")
        );

        final var saved = jpaRepository.save(product);
        final var found = jpaRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertInstanceOf(Book.class, found.get().getType());
        Book type = (Book) found.get().getType();
        assertEquals("Robert C. Martin", type.author());
        assertEquals("Software Engineering", type.genre());
    }


}