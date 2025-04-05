package local.dev.storemanager.domain.model.product;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Book")
public record Book(String author, String genre) implements ProductType {
    @Override
    public String label() {
        return "Book";
    }

    public String getType() {
        return "Book";
    }
}