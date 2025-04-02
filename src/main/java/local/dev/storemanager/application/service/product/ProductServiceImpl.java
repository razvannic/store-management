package local.dev.storemanager.application.service.product;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.exception.ProductNotFoundException;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.domain.service.ProductService;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
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
        Product product = mapper.toDomain(dto);
        Product saved = productRepository.save(product);

        cacheManager.getCache("product").put(saved.getId(), saved);
        cacheManager.getCache("products").clear();

        return saved;
    }

    @Override
    public Product findById(Long id) {
        final var cache = cacheManager.getCache("product");
        final var cached = cache.get(id, Product.class);

        if (cached != null) return cached;

        final var fromDb = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        cache.put(id, fromDb);
        return fromDb;
    }

    @Override
    public List<Product> findAll() {
        final var cache = cacheManager.getCache("products");
        final var cachedList = cache.get("all", List.class);

        if (cachedList != null) return cachedList;

        final var fromDb = productRepository.findAll();
        cache.put("all", fromDb);

        return fromDb;
    }

    @Override
    public Product updateProduct(Long id, ProductRequestDto dto) {
        //this uses cache manually. usage of annotations was dropped due to the fact that
        //calling a cached method from the same bean bypassed the proxy
        //so annotations like @Cacheable were ignored

        final var product = findById(id);
        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setQuantity(dto.quantity());

        final var saved = productRepository.save(product);

        cacheManager.getCache("product").put(id, saved);
        cacheManager.getCache("products").clear();

        return saved;
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        cacheManager.getCache("product").evict(id);
        cacheManager.getCache("products").clear();
    }
}
