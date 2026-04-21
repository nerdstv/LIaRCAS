package com.liarcas.processing.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.liarcas.models.LogEvent;
import com.liarcas.processing.document.LogEventDocument;
import com.liarcas.processing.repository.LogEventRepository;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.kafka.consumer.auto-offset-reset=earliest"
)
@Testcontainers
class LogConsumerKafkaElasticsearchIT {

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    static ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.12.0")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Autowired
    private LogEventRepository logEventRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
    }

    @Test
    void shouldConsumeKafkaMessageAndStoreItInElasticsearch() throws Exception {
        LogEvent event = new LogEvent(
                "log-123",
                "payment-service",
                "ERROR",
                "Database timeout",
                Instant.parse("2026-04-21T10:15:30Z")
        );

        send(event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<LogEventDocument> saved = logEventRepository.findById("log-123");

            assertThat(saved).isPresent();
            assertThat(saved.get().getId()).isEqualTo("log-123");
            assertThat(saved.get().getServiceName()).isEqualTo("payment-service");
            assertThat(saved.get().getLevel()).isEqualTo("ERROR");
            assertThat(saved.get().getMessage()).isEqualTo("Database timeout");
            assertThat(saved.get().getTimestamp()).isEqualTo(Instant.parse("2026-04-21T10:15:30Z"));
        });
    }

    private void send(LogEvent event) throws Exception {
        try (Producer<String, LogEvent> producer = new KafkaProducer<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName()
                )
        )) {
            producer.send(new ProducerRecord<>("raw-logs", event.getId(), event)).get(10, TimeUnit.SECONDS);
        }
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