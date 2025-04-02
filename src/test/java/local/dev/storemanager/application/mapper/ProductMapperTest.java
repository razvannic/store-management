package local.dev.storemanager.application.mapper;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductMapperTest {

    @Autowired
    private ProductMapper mapper;

    @Test
    void shouldMapDtoToDomain() {
        final var dto = new ProductRequestDto("Laptop", 999.99, 2);
        final var domain = mapper.toDomain(dto);

        assertEquals("Laptop", domain.getName());
        assertEquals(999.99, domain.getPrice());
        assertEquals(2, domain.getQuantity());
    }

    @Test
    void shouldMapDomainToEntity() {
        final var domain = Product.builder().id(10L).name("Tablet").price(300.0).quantity(5).build();
        final var entity = mapper.toEntity(domain);

        assertEquals(10L, entity.getId());
        assertEquals("Tablet", entity.getName());
        assertEquals(300.0, entity.getPrice());
        assertEquals(5, entity.getQuantity());
    }

    @Test
    void shouldMapEntityToDomain() {
        final var entity = new ProductEntity(20L, "Smartphone", 800.0, 1);
        final var domain = mapper.toDomain(entity);

        assertEquals(20L, domain.getId());
        assertEquals("Smartphone", domain.getName());
        assertEquals(800.0, domain.getPrice());
        assertEquals(1, domain.getQuantity());
    }

    @Test
    void shouldMapDomainToDto() {
        final var domain = Product.builder().id(30L).name("Headphones").price(150.0).quantity(7).build();
        final var dto = mapper.toResponseDto(domain);

        assertEquals("Headphones", dto.name());
        assertEquals(150.0, dto.price());
        assertEquals(7, dto.quantity());
    }
}
