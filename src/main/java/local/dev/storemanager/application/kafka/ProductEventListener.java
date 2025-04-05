package local.dev.storemanager.application.kafka;

import local.dev.storemanager.application.event.ProductEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static local.dev.storemanager.config.CacheNames.PRODUCTS;
import static local.dev.storemanager.config.KafkaTopics.STORE_GROUP;

@Slf4j
@Component
public class ProductEventListener {

    private final List<ProductEvent> receivedEvents = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = PRODUCTS, groupId = STORE_GROUP)
    public void listen(ProductEvent event) {
        log.info("Received product event: Type={}, Payload={}", event.getType(), event.getPayload());

        receivedEvents.add(event); // for test assert
    }

    public List<ProductEvent> getReceivedEvents() {
        return receivedEvents;
    }
}
