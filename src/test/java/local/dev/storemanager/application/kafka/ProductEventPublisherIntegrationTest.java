package local.dev.storemanager.application.kafka;

import local.dev.storemanager.application.event.ProductEvent;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.UUID;

import static local.dev.storemanager.config.KafkaTopics.PRODUCTS_TOPIC;
import static local.dev.storemanager.constants.EventTypes.PRODUCT_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = PRODUCTS_TOPIC)
class ProductEventPublisherIntegrationTest {

    @Autowired(required = false)
    private EmbeddedKafkaBroker embeddedKafka;
    private KafkaConsumer<String, ProductEvent> consumer;
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;
    private ProductEventPublisher publisher;

    @BeforeEach
    void setUp() {
        final var producerProps = KafkaTestUtils.producerProps(embeddedKafka);

        kafkaTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(
                        producerProps,
                        new org.apache.kafka.common.serialization.StringSerializer(),
                        new JsonSerializer<>()
                )
        );
        kafkaTemplate.setDefaultTopic(PRODUCTS_TOPIC);

        publisher = new ProductEventPublisher(kafkaTemplate);

        final var consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        consumerProps.put("auto.offset.reset", "earliest");

        consumer = (KafkaConsumer<String, ProductEvent>) new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(ProductEvent.class, false)
        ).createConsumer();

        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, PRODUCTS_TOPIC);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void shouldPublishProductEvent() {
        final var event = new ProductEvent(PRODUCT_CREATED, UUID.randomUUID().toString());

        publisher.publish(PRODUCTS_TOPIC, event);

        final var records = KafkaTestUtils.getRecords(consumer);
        final var record = records.iterator().next();

        assertEquals(PRODUCT_CREATED, record.value().getType());
    }
}
