package local.dev.storemanager.application.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import local.dev.storemanager.application.event.ProductEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static local.dev.storemanager.config.Metrics.PRODUCT_EVENTS_PUBLISHED_TOTAL;

@Component
public class ProductEventPublisher {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public ProductEventPublisher(KafkaTemplate<String, ProductEvent> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }


    public void publish(String topic, ProductEvent event) {
        kafkaTemplate.send(topic, event);

        Counter.builder(PRODUCT_EVENTS_PUBLISHED_TOTAL)
                .description("Total number of product events published to Kafka")
                .tag("topic", topic)
                .tag("eventType", event.getType())
                .register(meterRegistry)
                .increment();
    }
}
