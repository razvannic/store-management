package local.dev.storemanager.application.kafka;

import local.dev.storemanager.application.event.ProductEvent;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static local.dev.storemanager.config.KafkaTopics.PRODUCTS_TOPIC;
import static local.dev.storemanager.constants.EventTypes.PRODUCT_CREATED;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = PRODUCTS_TOPIC)
class ProductEventListenerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @Autowired
    private ProductEventListener listener;

    @Test
    void shouldPublishToAndConsumeFromKafka() {
        ProductEvent event = new ProductEvent(PRODUCT_CREATED, UUID.randomUUID().toString());
        kafkaTemplate.send(PRODUCTS_TOPIC, event);

        await()
                .atMost(5, SECONDS)
                .untilAsserted(() -> {
                    List<ProductEvent> events = listener.getReceivedEvents();
                    assertFalse(events.isEmpty());
                    assertEquals(PRODUCT_CREATED, events.get(0).getType());
                });
    }
}
