package local.dev.storemanager.application.kafka;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import local.dev.storemanager.application.event.ProductEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static local.dev.storemanager.config.KafkaTopics.PRODUCTS_TOPIC;
import static local.dev.storemanager.config.Metrics.PRODUCT_EVENTS_PUBLISHED_TOTAL;
import static local.dev.storemanager.constants.EventTypes.PRICE_CHANGED;
import static local.dev.storemanager.constants.EventTypes.PRODUCT_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProductEventPublisherTest {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate = mock();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final ProductEventPublisher publisher = new ProductEventPublisher(kafkaTemplate, meterRegistry);

    @Test
    void shouldPublishProductEventToKafka() {

        final var event = new ProductEvent(PRODUCT_CREATED, "Product payload");

        publisher.publish(PRODUCTS_TOPIC, event);

        final var captor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(kafkaTemplate, times(1)).send(eq(PRODUCTS_TOPIC), captor.capture());

        final var capturedEvent = captor.getValue();
        assertEquals(PRODUCT_CREATED, capturedEvent.getType());
        assertEquals("Product payload", capturedEvent.getPayload());
    }

    @Test
    void shouldPublishEventAndIncrementCounter() {
        final var event = new ProductEvent("1", PRICE_CHANGED);

        publisher.publish(PRODUCT_CREATED, event);

        verify(kafkaTemplate).send(PRODUCT_CREATED, event);

        final var counter = meterRegistry
                .get(PRODUCT_EVENTS_PUBLISHED_TOTAL)
                .tag("topic", PRODUCT_CREATED)
                .tag("eventType", event.getType())
                .counter();

        assertEquals(1.0, counter.count(), 0.01);
    }

}