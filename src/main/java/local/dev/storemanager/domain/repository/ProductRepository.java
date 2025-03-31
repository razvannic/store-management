package local.dev.storemanager.domain.repository;

import local.dev.storemanager.domain.model.Product;

public interface ProductRepository {
    Product save(Product product);
}