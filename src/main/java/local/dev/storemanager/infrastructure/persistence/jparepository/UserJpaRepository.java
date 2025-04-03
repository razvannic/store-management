package local.dev.storemanager.infrastructure.persistence.jparepository;

import local.dev.storemanager.infrastructure.persistence.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<AppUser, Long> {
    @Query("SELECT u FROM AppUser u WHERE LOWER(u.username) = LOWER(:username)")
    Optional<AppUser> findByUsernameIgnoreCase(@Param("username") String username);

}
