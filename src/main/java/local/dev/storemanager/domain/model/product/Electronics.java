package local.dev.storemanager.domain.model.product;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Electronics")
public record Electronics(String brand, String warranty) implements ProductType {
    @Override
    public String label() {
        return "Electronics";
    }

    public String getType() {
        return "Electronics";
    }
}
