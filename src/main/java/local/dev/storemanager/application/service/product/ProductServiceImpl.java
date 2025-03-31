package local.dev.storemanager.application.service.product;

import local.dev.storemanager.application.dto.ProductDto;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.domain.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    public void addProduct(ProductDto dto) {
        final var product = productMapper.toDomain(dto);
        productRepository.save(product);
    }
}
