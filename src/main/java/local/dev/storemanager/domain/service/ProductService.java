package local.dev.storemanager.domain.service;


import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.domain.model.product.Product;

import java.util.List;

public interface ProductService {
    Product addProduct(ProductRequestDto dto);

    Product findById(Long id);

    List<Product> findAll();

    List<Product> findAllFiltered(String type, String author, String brand, String size);

    Product updateProduct(Long id, ProductRequestDto dto);

    void deleteProduct(Long id);
}
