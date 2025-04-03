package local.dev.storemanager.infrastructure.persistence.jparepository;

import local.dev.storemanager.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
}
