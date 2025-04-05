package local.dev.storemanager.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProductRequestDto(
        @NotBlank(message = "Product name must not be blank")
        String name,

        @Positive(message = "Price must be greater than 0")
        double price,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,
        String type,
        String author,
        String genre,
        String brand,
        String warranty,
        String size,
        String material
) {
}