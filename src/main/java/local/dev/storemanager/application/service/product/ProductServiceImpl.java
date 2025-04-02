package local.dev.storemanager.application.service.product;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.domain.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper mapper) {
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Override
    public Product addProduct(ProductRequestDto dto) {
        Product product = mapper.toDomain(dto);
        return productRepository.save(product);
    }

    @Override
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product updateProduct(Long id, ProductRequestDto dto) {
        Product existing = findById(id);
        existing.setName(dto.name());
        existing.setPrice(dto.price());
        existing.setQuantity(dto.quantity());
        return productRepository.save(existing);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
