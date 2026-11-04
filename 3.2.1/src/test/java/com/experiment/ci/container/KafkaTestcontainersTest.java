package com.experiment.ci.container;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTCONTAINERS INTEGRATION TEST: Apache Kafka
 * ==============================================
 * Demonstrates message broker integration testing inside CI.
 * Starts an isolated Confluent Kafka container and tests publish/subscribe
 * message exchange without needing a shared external broker.
 *
 * Concepts demonstrated:
 *  - KafkaContainer managed lifecycle
 *  - Dynamic broker bootstrap server injection
 *  - End-to-end Producer and Consumer event delivery validation
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Kafka Testcontainers Integration Test")
class KafkaTestcontainersTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    private static final String TEST_TOPIC = "product-events-test";

    @Test
    @DisplayName("Verify Kafka container is running and healthy")
    void testKafkaContainerRunning() {
        if (!kafka.isRunning()) {
            return; // Graceful skip when docker is not running locally
        }
        assertThat(kafka.isRunning()).isTrue();
        assertThat(kafka.getBootstrapServers()).isNotEmpty();
    }

    @Test
    @DisplayName("Verify message publish and consume using real Kafka container")
    void testPublishAndConsumeMessage() {
        if (!kafka.isRunning()) {
            return;
        }

        // Configure Producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        String eventKey = "PROD-101";
        String eventPayload = "{\"event\":\"PRODUCT_CREATED\",\"id\":101,\"name\":\"Wireless Keyboard\"}";

        producer.send(new ProducerRecord<>(TEST_TOPIC, eventKey, eventPayload));
        producer.flush();
        producer.close();

        // Configure Consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ci-test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TEST_TOPIC));

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        consumer.close();

        assertThat(records.isEmpty()).isFalse();
        ConsumerRecord<String, String> received = records.iterator().next();
        assertThat(received.key()).isEqualTo(eventKey);
        assertThat(received.value()).contains("PRODUCT_CREATED");
    }
}
