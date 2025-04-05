package local.dev.storemanager.application.mapper;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.dto.ProductResponseDto;
import local.dev.storemanager.domain.model.product.*;
import local.dev.storemanager.infrastructure.persistence.config.PostgresTestContainer;
import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class ProductMapperTest {

    @Autowired
    private ProductMapper mapper;

    @Test
    void shouldMapDtoToDomain() {
        var dto = new ProductRequestDto(
                "Effective Java", 49.99, 3,
                "Book", "Joshua Bloch", "Programming",
                null, null, null, null
        );

        var domain = mapper.toDomain(dto);

        assertEquals("Effective Java", domain.getName());
        assertEquals(49.99, domain.getPrice());
        assertEquals(3, domain.getQuantity());

        assertInstanceOf(Book.class, domain.getType());
        var book = (Book) domain.getType();
        assertEquals("Joshua Bloch", book.author());
        assertEquals("Programming", book.genre());
    }

    @Test
    void shouldMapDomainToEntity() {
        var type = new Electronics("Logitech", "2 years");
        var domain = Product.builder()
                .id(10L)
                .name("Mouse")
                .price(45.0)
                .quantity(12)
                .type(type)
                .build();

        var entity = mapper.toEntity(domain);

        assertEquals(10L, entity.getId());
        assertEquals("Mouse", entity.getName());
        assertEquals(45.0, entity.getPrice());
        assertEquals(12, entity.getQuantity());
        assertEquals(type, entity.getType());
    }

    @Test
    void shouldMapEntityToDomain() {
        var type = new Clothing("L", "Cotton");
        var entity = new ProductEntity(20L, "T-Shirt", 25.0, 50, type);

        var domain = mapper.toDomain(entity);

        assertEquals(20L, domain.getId());
        assertEquals("T-Shirt", domain.getName());
        assertEquals(25.0, domain.getPrice());
        assertEquals(50, domain.getQuantity());
        assertTrue(domain.getType() instanceof Clothing);
        var clothing = (Clothing) domain.getType();
        assertEquals("L", clothing.size());
        assertEquals("Cotton", clothing.material());
    }

    @Test
    void shouldMapDomainToDto() {
        var type = new Book("Martin Fowler", "Software Engineering");
        var domain = Product.builder()
                .id(30L)
                .name("Refactoring")
                .price(65.0)
                .quantity(5)
                .type(type)
                .build();

        var dto = mapper.toResponseDto(domain);

        assertEquals(30L, dto.id());
        assertEquals("Refactoring", dto.name());
        assertEquals(65.0, dto.price());
        assertEquals(5, dto.quantity());
        assertEquals("Book", dto.type());
        assertEquals(type, dto.typeDetails());
    }
}
