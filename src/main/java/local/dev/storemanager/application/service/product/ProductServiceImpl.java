package local.dev.storemanager.application.service.product;

import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.exception.ProductNotFoundException;
import local.dev.storemanager.application.kafka.ProductEventPublisher;
import local.dev.storemanager.application.event.ProductEvent;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.domain.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

import static local.dev.storemanager.config.CacheNames.PRODUCT;
import static local.dev.storemanager.config.CacheNames.PRODUCTS;
import static local.dev.storemanager.config.KafkaTopics.PRODUCTS_TOPIC;
import static local.dev.storemanager.constants.EventTypes.PRICE_CHANGED;
import static local.dev.storemanager.constants.EventTypes.PRODUCT_CREATED;

@Slf4j
@Service("productServiceImpl")
/**
 * This service uses manual caching via CacheManager.
 * Annotations like @Cacheable were avoided due to Spring proxy limitations
 * when calling methods within the same bean.
 */
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final CacheManager cacheManager;
    private final ProductEventPublisher publisher;

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductMapper mapper,
                              CacheManager cacheManager,
                              ProductEventPublisher publisher) {
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
        this.publisher = publisher;
    }

    @Override
    public Product addProduct(ProductRequestDto dto) {
        log.info("Adding new product: {}", dto.name());

        final var saved = productRepository.save(mapper.toDomain(dto));
        log.debug("Product saved with ID: {}", saved.getId());

        getCache(PRODUCT).put(saved.getId(), saved);
        evictAll(PRODUCTS);

        publisher.publish(PRODUCTS_TOPIC, new ProductEvent(PRODUCT_CREATED, saved));
        return saved;
    }

    @Override
    public Product findById(Long id) {
        log.info("Fetching product with ID: {}", id);

        final var cache = getCache(PRODUCT);
        final var cached = cache.get(id, Product.class);

        if (cached != null) return cached;

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        log.debug("Product retrieved from DB: {}", product.getId());
        cache.put(id, product);
        return product;
    }

    @Override
    public List<Product> findAll() {
        log.info("Fetching all products");

        final var cache = getCache(PRODUCTS);
        final var cachedList = cache.get("all", List.class);

        if (cachedList != null) return cachedList;

        final var products = productRepository.findAll();
        log.info("Retrieved {} products from DB", products.size());

        products.forEach(p -> log.debug("Product: {}", p));
        cache.put("all", products);

        return products;
    }

    @Override
    public Product updateProduct(Long id, ProductRequestDto dto) {
        log.info("Updating product with ID: {}", id);

        final var product = findById(id);
        final var priceChanged = Double.compare(product.getPrice(), dto.price()) != 0;

        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setQuantity(dto.quantity());

        Product saved = productRepository.save(product);
        log.debug("Product updated: {}", saved.getId());

        getCache(PRODUCT).put(id, saved);
        evictAll(PRODUCTS);

        if (priceChanged) {
            publisher.publish(PRODUCTS_TOPIC, new ProductEvent(PRICE_CHANGED, String.valueOf(saved.getId())));
            log.info("Published PRICE_CHANGED event for product ID: {}", saved.getId());
        }

        return saved;
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);

        productRepository.deleteById(id);

        getCache(PRODUCT).evict(id);
        evictAll(PRODUCTS);

        log.info("Deleted product with ID: {}", id);
    }

    private Cache getCache(String name) {
        return cacheManager.getCache(name);
    }

    private void evictAll(String cacheName) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
