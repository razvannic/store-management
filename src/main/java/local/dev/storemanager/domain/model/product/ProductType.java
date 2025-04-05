package local.dev.storemanager.domain.model.product;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Book.class, name = "Book"),
        @JsonSubTypes.Type(value = Electronics.class, name = "Electronics"),
        @JsonSubTypes.Type(value = Clothing.class, name = "Clothing")
})
public sealed interface ProductType permits Electronics, Clothing, Book {
    String label();
}