package local.dev.storemanager;

import local.dev.storemanager.infrastructure.persistence.config.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
class StoreManagerApplicationTests {
    @Test
    void contextLoads() {
    }

}
