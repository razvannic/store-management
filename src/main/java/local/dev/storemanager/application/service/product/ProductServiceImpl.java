package local.dev.storemanager.application.service.product;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.exception.ProductNotFoundException;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.domain.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("productServiceImpl")
/**
 * This service uses cache manually via CacheManager.
 * <p>
 * Usage of annotations like @Cacheable was dropped because calling cached methods from
 * within the same bean bypasses Spring's proxy mechanism, causing annotations to be ignored.
 * <p>
 * Manual caching avoids self-invocation issues and gives full control.
 */
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final CacheManager cacheManager;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper mapper, CacheManager cacheManager) {
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
    }

    @Override
    public Product addProduct(ProductRequestDto dto) {
        log.info("Adding new product: {}", dto.name());
        Product product = mapper.toDomain(dto);
        Product saved = productRepository.save(product);
        log.debug("Product saved with ID: {}", saved.getId());

        cacheManager.getCache("product").put(saved.getId(), saved);
        cacheManager.getCache("products").clear();

        return saved;
    }

    @Override
    public Product findById(Long id) {
        log.info("Fetching product with ID: {}", id);

        final var cache = cacheManager.getCache("product");
        final var cached = cache.get(id, Product.class);

        if (cached != null) return cached;

        final var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        log.debug("Product retrieved from db with ID: {}", product.getId());

        cache.put(id, product);
        return product;
    }

    @Override
    public List<Product> findAll() {
        log.info("Fetching all products");

        final var cache = cacheManager.getCache("products");
        final var cachedList = cache.get("all", List.class);

        if (cachedList != null) return cachedList;

        final var products = productRepository.findAll();
        log.info("Retrieved {} products from DB", products.size());
        products.forEach(product -> log.debug("Product: {}", product));

        cache.put("all", products);

        return products;
    }

    @Override
    public Product updateProduct(Long id, ProductRequestDto dto) {
        log.info("Updating product with ID: {}", id);

        final var product = findById(id);
        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setQuantity(dto.quantity());

        final var saved = productRepository.save(product);
        log.debug("Product with ID: {} updated.", product.getId());

        cacheManager.getCache("product").put(id, saved);
        cacheManager.getCache("products").clear();

        return saved;
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Removing product with ID: {}", id);
        productRepository.deleteById(id);
        log.info("Product with ID: {} removed", id);

        cacheManager.getCache("product").evict(id);
        cacheManager.getCache("products").clear();
    }
}
