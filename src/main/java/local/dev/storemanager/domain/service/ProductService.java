package local.dev.storemanager.domain.service;


import local.dev.storemanager.application.dto.ProductDto;
import local.dev.storemanager.domain.model.Product;

import java.util.List;

public interface ProductService {
    Product addProduct(ProductDto dto);

    Product findById(Long id);

    List<Product> findAll();

    Product updateProduct(Long id, ProductDto dto);

    void deleteProduct(Long id);
}
