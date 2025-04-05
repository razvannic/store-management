package local.dev.storemanager.application.mapper;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.dto.ProductResponseDto;
import local.dev.storemanager.domain.model.product.Product;
import local.dev.storemanager.domain.model.product.ProductType;
import local.dev.storemanager.domain.model.product.ProductTypeFactory;
import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductRequestDto dto) {
        return Product.builder()
                .name(dto.name())
                .price(dto.price())
                .quantity(dto.quantity())
                .type(ProductTypeFactory.from(dto))
                .build();
    }

    public Product toDomain(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .type(entity.getType())
                .build();
    }

    public ProductEntity toEntity(Product domain) {
        return ProductEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .price(domain.getPrice())
                .quantity(domain.getQuantity())
                .type(domain.getType())
                .build();
    }


    public ProductResponseDto toResponseDto(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getQuantity(),
                product.getType() != null ? product.getType().label() : null,
                product.getType()
        );
    }

}
