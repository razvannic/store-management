package local.dev.storemanager.domain.model.product;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Clothing")
public record Clothing(String size, String material) implements ProductType {
    @Override
    public String label() {
        return "Clothing";
    }

    public String getType() {
        return "Clothing";
    }
}