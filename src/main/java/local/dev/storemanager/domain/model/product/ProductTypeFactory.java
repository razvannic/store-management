package local.dev.storemanager.domain.model.product;

import local.dev.storemanager.application.dto.ProductRequestDto;

public class ProductTypeFactory {

    public static ProductType from(ProductRequestDto dto) {
        if (dto.type() == null) {
            return null;
        }

        return switch (dto.type()) {
            case "Book" -> new Book(dto.author(), dto.genre());
            case "Electronics" -> new Electronics(dto.brand(), dto.warranty());
            case "Clothing" -> new Clothing(dto.size(), dto.material());
            default -> null; // or throw IllegalArgumentException if needed
        };
    }
}

