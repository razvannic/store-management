package local.dev.storemanager.application.dto;

import local.dev.storemanager.domain.model.product.ProductType;

public record ProductResponseDto(
        Long id,
        String name,
        double price,
        int quantity,
        String type,
        ProductType typeDetails
) {}
