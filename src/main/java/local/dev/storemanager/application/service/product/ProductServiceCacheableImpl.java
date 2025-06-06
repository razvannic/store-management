package local.dev.storemanager.application.service.product;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.exception.ProductNotFoundException;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.domain.model.product.Book;
import local.dev.storemanager.domain.model.product.Clothing;
import local.dev.storemanager.domain.model.product.Electronics;
import local.dev.storemanager.domain.model.product.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.domain.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static local.dev.storemanager.config.CacheNames.PRODUCT;
import static local.dev.storemanager.config.CacheNames.PRODUCTS;

@Service("productServiceCacheableImpl")
public class ProductServiceCacheableImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public ProductServiceCacheableImpl(ProductRepository productRepository, ProductMapper mapper) {
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Override
    @CachePut(value = "product", key = "#result.id")
    public Product addProduct(ProductRequestDto dto) {
        final var product = mapper.toDomain(dto);
        return productRepository.save(product);
    }

    @Override
    @Cacheable(value = "product", key = "#id")
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Cacheable(PRODUCTS)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findAllFiltered(String type, String author, String brand, String size) {

        final var allProducts = productRepository.findAll();

        return allProducts.stream()
                .filter(product -> {
                    final var productType = product.getType();

                    if (type != null) {
                        if (productType == null || !type.equalsIgnoreCase(productType.label())) {
                            return false;
                        }
                    }

                    if (productType instanceof Book book) {
                        return author == null || book.author().equalsIgnoreCase(author);
                    } else if (productType instanceof Electronics electronics) {
                        return brand == null || electronics.brand().equalsIgnoreCase(brand);
                    } else if (productType instanceof Clothing clothing) {
                        return size == null || clothing.size().equalsIgnoreCase(size);
                    }

                    // Product has no type or no filter matched
                    return type == null && author == null && brand == null && size == null;
                })
                .toList();
    }

    @Override
    @CacheEvict(value = {PRODUCT, PRODUCTS}, allEntries = true)
    public Product updateProduct(Long id, ProductRequestDto dto) {
        final var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setQuantity(dto.quantity());
        return productRepository.save(product);
    }

    @Override
    @CacheEvict(value = {PRODUCT, PRODUCTS}, allEntries = true)
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}