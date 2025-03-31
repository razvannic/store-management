package local.dev.storemanager.application.mapper;

import local.dev.storemanager.application.dto.ProductDto;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.infrastructure.persistence.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductDto dto) {
        return Product.builder()
                .name(dto.name())
                .price(dto.price())
                .quantity(dto.quantity())
                .build();
    }

    public Product toDomain(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .build();
    }

    public ProductEntity toEntity(Product domain) {
        return ProductEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .price(domain.getPrice())
                .quantity(domain.getQuantity())
                .build();
    }

    public ProductDto toDto(Product domain) {
        return new ProductDto(
                domain.getName(),
                domain.getPrice(),
                domain.getQuantity()
        );
    }
}
