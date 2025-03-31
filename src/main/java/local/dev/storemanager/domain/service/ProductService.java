package local.dev.storemanager.domain.service;


import local.dev.storemanager.application.dto.ProductDto;

public interface ProductService {
    void addProduct(ProductDto dto);
}
