package local.dev.storemanager.infrastructure.persistence;


import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.repository.ProductRepository;
import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final ProductMapper mapper;

    public ProductRepositoryImpl(ProductJpaRepository jpaRepository, ProductMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {
        final var entity = mapper.toEntity(product);
        final var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
