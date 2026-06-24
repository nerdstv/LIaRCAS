package com.liarcas.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.liarcas.processing.document.LogEventDocument;
import com.liarcas.processing.index.IndexNameUtil;

/**
 * Integration test for tenant-scoped Elasticsearch indexing.
 * Verifies that documents for different tenants are stored in separate indices.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class TenantScopedDocumentServiceIT {

    @Container
    static ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.12.0")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Autowired
    private TenantScopedDocumentService tenantScopedDocumentService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
    }

    @Test
    void shouldSaveDocumentToTenant001Index() {
        LogEventDocument doc = new LogEventDocument(
                "log-tenant-001-1",
                "tenant-001",
                "service-a",
                null,
                null,
                null,
                null,
                null,
                "ERROR",
                "Error in service A",
                null,
                null,
                Instant.now()
        );

        LogEventDocument saved = tenantScopedDocumentService.save(doc);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("log-tenant-001-1");

        // Verify the document is in the tenant-001 index
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String indexName = IndexNameUtil.getTenantIndexName("tenant-001");
            org.springframework.data.elasticsearch.core.mapping.IndexCoordinates indexCoordinates = 
                org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of(indexName);
            try {
                LogEventDocument retrieved = elasticsearchOperations.get(
                        "log-tenant-001-1",
                        LogEventDocument.class,
                        indexCoordinates
                );
                assertThat(retrieved).isNotNull();
                assertThat(retrieved.getTenantId()).isEqualTo("tenant-001");
                assertThat(retrieved.getServiceName()).isEqualTo("service-a");
            } catch (org.springframework.data.elasticsearch.NoSuchIndexException e) {
                throw new AssertionError("Index or document not found yet: " + e.getMessage());
            }
        });
    }

    @Test
    void shouldSaveDocumentToTenant002Index() {
        LogEventDocument doc = new LogEventDocument(
                "log-tenant-002-1",
                "tenant-002",
                "service-b",
                null,
                null,
                null,
                null,
                null,
                "WARN",
                "Warning in service B",
                null,
                null,
                Instant.now()
        );

        LogEventDocument saved = tenantScopedDocumentService.save(doc);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("log-tenant-002-1");

        // Verify the document is in the tenant-002 index
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String indexName = IndexNameUtil.getTenantIndexName("tenant-002");
            org.springframework.data.elasticsearch.core.mapping.IndexCoordinates indexCoordinates = 
                org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of(indexName);
            try {
                LogEventDocument retrieved = elasticsearchOperations.get(
                        "log-tenant-002-1",
                        LogEventDocument.class,
                        indexCoordinates
                );
                assertThat(retrieved).isNotNull();
                assertThat(retrieved.getTenantId()).isEqualTo("tenant-002");
                assertThat(retrieved.getServiceName()).isEqualTo("service-b");
            } catch (org.springframework.data.elasticsearch.NoSuchIndexException e) {
                throw new AssertionError("Index or document not found yet: " + e.getMessage());
            }
        });
    }

    @Test
    void shouldKeepTenantDataIsolated() {
        // Save a document to tenant-001
        LogEventDocument doc1 = new LogEventDocument(
                "log-001",
                "tenant-001",
                "service-1",
                null,
                null,
                null,
                null,
                null,
                "INFO",
                "Tenant 001 event",
                null,
                null,
                Instant.now()
        );
        tenantScopedDocumentService.save(doc1);

        // Save a document to tenant-002
        LogEventDocument doc2 = new LogEventDocument(
                "log-002",
                "tenant-002",
                "service-2",
                null,
                null,
                null,
                null,
                null,
                "INFO",
                "Tenant 002 event",
                null,
                null,
                Instant.now()
        );
        tenantScopedDocumentService.save(doc2);

        // Verify tenant-001 document is in logs-tenant-001 and not in logs-tenant-002
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String index001 = IndexNameUtil.getTenantIndexName("tenant-001");
            String index002 = IndexNameUtil.getTenantIndexName("tenant-002");
            org.springframework.data.elasticsearch.core.mapping.IndexCoordinates indexCoordinates001 = 
                org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of(index001);
            org.springframework.data.elasticsearch.core.mapping.IndexCoordinates indexCoordinates002 = 
                org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of(index002);

            try {
                // Document 1 should be in index001
                LogEventDocument retrieved1 = elasticsearchOperations.get(
                        "log-001",
                        LogEventDocument.class,
                        indexCoordinates001
                );
                assertThat(retrieved1).isNotNull();
                assertThat(retrieved1.getTenantId()).isEqualTo("tenant-001");

                // Document 2 should be in index002
                LogEventDocument retrieved2 = elasticsearchOperations.get(
                        "log-002",
                        LogEventDocument.class,
                        indexCoordinates002
                );
                assertThat(retrieved2).isNotNull();
                assertThat(retrieved2.getTenantId()).isEqualTo("tenant-002");
            } catch (org.springframework.data.elasticsearch.NoSuchIndexException e) {
                throw new AssertionError("Index or document not found yet: " + e.getMessage());
            }
        });
    }

    @Test
    void shouldRejectDocumentWithoutTenantId() {
        LogEventDocument doc = new LogEventDocument(
                "log-no-tenant",
                "payment-service",
                "ERROR",
                "Error without tenant",
                Instant.now()
        );
        doc.setTenantId(null);

        assertThat(catchException(() -> tenantScopedDocumentService.save(doc)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId cannot be null or blank");
    }

    private Exception catchException(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}
