package local.dev.storemanager.application.kafka;

import local.dev.storemanager.application.event.ProductEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static local.dev.storemanager.config.KafkaTopics.PRODUCTS_TOPIC;
import static local.dev.storemanager.constants.EventTypes.PRODUCT_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProductEventPublisherTest {

    @Test
    void shouldPublishProductEventToKafka() {
        final var kafkaTemplate = mock(KafkaTemplate.class);
        final var publisher = new ProductEventPublisher(kafkaTemplate);

        final var event = new ProductEvent(PRODUCT_CREATED, "Product payload");

        publisher.publish(PRODUCTS_TOPIC, event);

        final var captor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(kafkaTemplate, times(1)).send(eq(PRODUCTS_TOPIC), captor.capture());

        final var capturedEvent = captor.getValue();
        assertEquals(PRODUCT_CREATED, capturedEvent.getType());
        assertEquals("Product payload", capturedEvent.getPayload());
    }
}