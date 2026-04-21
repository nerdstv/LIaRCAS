package com.liarcas.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.liarcas.models.LogEvent;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LogControllerKafkaIT {

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private Consumer<String, LogEvent> consumer;

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void shouldPublishReceivedLogToKafka() {
        consumer = createConsumer();
        consumer.subscribe(List.of("raw-logs"));

        LogEvent request = new LogEvent();
        request.setServiceName("payment-service");
        request.setComponent("db-client");
        request.setEnvironment("prod");
        request.setServiceVersion("1.4.2");
        request.setInstanceId("payment-pod-7");
        request.setTraceId("trace-abc-123");
        request.setLevel("ERROR");
        request.setMessage("Database timeout");
        request.setExceptionType("SQLTransientConnectionException");
        request.setStackTraceHash("sth-9f8c2d");

        LogEvent response = restTemplate.postForObject(
                "http://localhost:" + port + "/logs",
                request,
                LogEvent.class
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotBlank();
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getServiceName()).isEqualTo("payment-service");
        assertThat(response.getLevel()).isEqualTo("ERROR");
        assertThat(response.getMessage()).isEqualTo("Database timeout");
        assertThat(response.getComponent()).isEqualTo("db-client");
        assertThat(response.getEnvironment()).isEqualTo("prod");
        assertThat(response.getServiceVersion()).isEqualTo("1.4.2");
        assertThat(response.getInstanceId()).isEqualTo("payment-pod-7");
        assertThat(response.getTraceId()).isEqualTo("trace-abc-123");
        assertThat(response.getExceptionType()).isEqualTo("SQLTransientConnectionException");
        assertThat(response.getStackTraceHash()).isEqualTo("sth-9f8c2d");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, LogEvent> records = consumer.poll(Duration.ofMillis(500));
            assertThat(records.count()).isGreaterThan(0);

            LogEvent published = records.iterator().next().value();
            assertThat(published.getId()).isEqualTo(response.getId());
            assertThat(published.getTimestamp()).isEqualTo(response.getTimestamp());
            assertThat(published.getServiceName()).isEqualTo("payment-service");
            assertThat(published.getLevel()).isEqualTo("ERROR");
            assertThat(published.getMessage()).isEqualTo("Database timeout");
            assertThat(published.getComponent()).isEqualTo("db-client");
            assertThat(published.getEnvironment()).isEqualTo("prod");
            assertThat(published.getServiceVersion()).isEqualTo("1.4.2");
            assertThat(published.getInstanceId()).isEqualTo("payment-pod-7");
            assertThat(published.getTraceId()).isEqualTo("trace-abc-123");
            assertThat(published.getExceptionType()).isEqualTo("SQLTransientConnectionException");
            assertThat(published.getStackTraceHash()).isEqualTo("sth-9f8c2d");
        });
    }

    private Consumer<String, LogEvent> createConsumer() {
        JsonDeserializer<LogEvent> valueDeserializer = new JsonDeserializer<>(LogEvent.class);
        valueDeserializer.addTrustedPackages("com.liarcas.models");

        return new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, "log-ingestion-it",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @TestConfiguration
    static class TopicConfiguration {

        @Bean
        NewTopic rawLogsTopic() {
            return TopicBuilder.name("raw-logs")
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }
}