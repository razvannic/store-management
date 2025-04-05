package local.dev.storemanager.application.kafka;

import local.dev.storemanager.application.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    public void publish(String topic, ProductEvent event) {
        kafkaTemplate.send(topic, event);
    }
}
