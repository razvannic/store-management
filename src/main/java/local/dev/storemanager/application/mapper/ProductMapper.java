package local.dev.storemanager.application.mapper;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.dto.ProductResponseDto;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductRequestDto dto) {
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

    public ProductResponseDto toResponseDto(Product domain) {
        return new ProductResponseDto(
                domain.getId(),
                domain.getName(),
                domain.getPrice(),
                domain.getQuantity()
        );
    }
}
